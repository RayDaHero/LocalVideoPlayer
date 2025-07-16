package com.example.localvideoplayer.repository

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.localvideoplayer.data.ThumbnailItem
import com.example.localvideoplayer.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class VideoRepository(private val context: Context) {

    suspend fun getVideos(onlyExported: Boolean = false): List<VideoItem> {
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

            val selection = if (onlyExported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.RELATIVE_PATH + " LIKE ?"
            } else {
                null
            }

            val selectionArgs = if (onlyExported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf("%LocalVideoPlayer%")
            } else {
                null
            }

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
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

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        )

                        try {
                            context.contentResolver.openInputStream(contentUri)?.close()
                            videoList.add(
                                VideoItem(
                                    uri = contentUri,
                                    name = name,
                                    duration = formatDuration(durationMs),
                                    size = size,
                                    resolution = if (width > 0 && height > 0) "$width x $height" else "Unknown"
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("VideoRepository", "Could not open video file", e)
                        }
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