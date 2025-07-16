package com.example.localvideoplayer.repository

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.localvideoplayer.data.ExportStatus
import com.example.localvideoplayer.data.ThumbnailItem
import com.example.localvideoplayer.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class VideoRepository(private val context: Context) {

    private val _exportEvent = MutableLiveData<ExportStatus>(ExportStatus.Idle)
    val exportEvent: LiveData<ExportStatus> = _exportEvent

    suspend fun getVideos(onlyExported: Boolean = false, searchQuery: String? = null): List<VideoItem> {
        return withContext(Dispatchers.IO) {
            val videoList = mutableListOf<VideoItem>()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
            )

            val selectionClauses = mutableListOf<String>()
            val selectionArgsList = mutableListOf<String>()

            if (onlyExported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                selectionClauses.add(MediaStore.Video.Media.RELATIVE_PATH + " LIKE ?")
                selectionArgsList.add("%LocalVideoPlayer%")
            }

            if (!searchQuery.isNullOrBlank()) {
                selectionClauses.add(MediaStore.Video.Media.DISPLAY_NAME + " LIKE ?")
                selectionArgsList.add("%$searchQuery%")
            }

            val selection = selectionClauses.joinToString(" AND ")
            val selectionArgs = selectionArgsList.toTypedArray()

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    if (selection.isNotEmpty()) selection else null,
                    if (selectionArgs.isNotEmpty()) selectionArgs else null,
                    "${MediaStore.Video.Media.DATE_ADDED} DESC"
                )

                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val widthColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                    val heightColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val name = it.getString(nameColumn)
                        val durationMs = it.getLong(durationColumn)
                        val size = it.getLong(sizeColumn)
                        val width = it.getInt(widthColumn)
                        val height = it.getInt(heightColumn)

                        if (durationMs < 100) continue

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        )

                        videoList.add(
                            VideoItem(
                                uri = contentUri,
                                name = name,
                                duration = formatDuration(durationMs),
                                size = size,
                                resolution = if (width > 0 && height > 0) "$width x $height" else "Unknown"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("VideoRepository", "Error getting videos", e)
            }
            videoList
        }
    }

    fun generateThumbnails(videoUri: Uri, interval: Int): Flow<ThumbnailItem> = flow {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0
            val intervalUs = TimeUnit.SECONDS.toMicros(interval.toLong())
            var currentTimeUs = 0L

            while (currentTimeUs < TimeUnit.MILLISECONDS.toMicros(durationMs)) {
                val bitmap = retriever.getFrameAtTime(
                    currentTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (bitmap != null) {
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 120, 120, false)
                    bitmap.recycle()
                    emit(ThumbnailItem(scaledBitmap, TimeUnit.MICROSECONDS.toMillis(currentTimeUs)))
                }
                currentTimeUs += intervalUs
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error generating thumbnails", e)
        } finally {
            retriever.release()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Exports a video clip using the specified time range
     * @param uri The URI of the source video
     * @param startTimeMs Start time in milliseconds
     * @param endTimeMs End time in milliseconds
     */
    fun exportVideoClip(uri: Uri, startTimeMs: Long, endTimeMs: Long) {
        try {
            if (startTimeMs < 0 || endTimeMs <= startTimeMs) {
                _exportEvent.value = ExportStatus.Error("Invalid time range specified")
                return
            }

            _exportEvent.value = ExportStatus.InProgress

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.localvideoplayer.workers.VideoExportWorker>()
                .setInputData(
                    androidx.work.workDataOf(
                        com.example.localvideoplayer.workers.VideoExportWorker.KEY_INPUT_URI to uri.toString(),
                        com.example.localvideoplayer.workers.VideoExportWorker.KEY_START_MS to startTimeMs,
                        com.example.localvideoplayer.workers.VideoExportWorker.KEY_END_MS to endTimeMs
                    )
                )
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(workRequest)

        } catch (e: Exception) {
            Log.e("VideoRepository", "Failed to start video export", e)
            _exportEvent.value = ExportStatus.Error("Failed to start export: ${e.message}")
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}