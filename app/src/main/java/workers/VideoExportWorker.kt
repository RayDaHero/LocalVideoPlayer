package com.example.localvideoplayer.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.localvideoplayer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoExportWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_INPUT_URI = "KEY_INPUT_URI"
        const val KEY_START_MS = "KEY_START_MS"
        const val KEY_END_MS = "KEY_END_MS"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "VideoExportChannel"
    }

    override suspend fun doWork(): Result {
        val inputUriString = inputData.getString(KEY_INPUT_URI) ?: return Result.failure()
        val startTimeUs = inputData.getLong(KEY_START_MS, 0) * 1000
        val endTimeUs = inputData.getLong(KEY_END_MS, 0) * 1000
        val inputUri = Uri.parse(inputUriString)

        return withContext(Dispatchers.IO) {
            try {
                setForeground(createForegroundInfo("Starting export..."))

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val outputFileName = "Exported_Scene_$timeStamp.mp4"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/LocalVideoPlayer")
                    }
                }

                val resolver = applicationContext.contentResolver
                val outputUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure()

                resolver.openFileDescriptor(outputUri, "w")?.use { pfd ->
                    trimVideo(
                        context = applicationContext,
                        inputUri = inputUri,
                        outputPfd = pfd.fileDescriptor,
                        startUs = startTimeUs,
                        endUs = endTimeUs,
                        onProgress = { progress ->
                            val notification = createNotification("Exporting... ${progress.toInt()}%")
                            notificationManager.notify(NOTIFICATION_ID, notification)
                        }
                    )
                }

                showCompletionNotification()
                Result.success()

            } catch (e: Exception) {
                Log.e("VideoExportWorker", "Export failed with exception", e)
                Result.failure()
            }
        }
    }

    @Throws(IOException::class)
    private fun trimVideo(
        context: Context,
        inputUri: Uri,
        outputPfd: java.io.FileDescriptor,
        startUs: Long,
        endUs: Long,
        onProgress: (Float) -> Unit
    ) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, inputUri, null)
            muxer = MediaMuxer(outputPfd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackIndexMap = mutableMapOf<Int, Int>()
            var videoTrackIndex = -1
            var audioTrackIndex = -1

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (videoTrackIndex == -1 && mime.startsWith("video/")) {
                    videoTrackIndex = i
                } else if (audioTrackIndex == -1 && mime.startsWith("audio/")) {
                    audioTrackIndex = i
                }
            }

            if (videoTrackIndex != -1) {
                extractor.selectTrack(videoTrackIndex)
                trackIndexMap[videoTrackIndex] = muxer.addTrack(extractor.getTrackFormat(videoTrackIndex))
            }
            if (audioTrackIndex != -1) {
                extractor.selectTrack(audioTrackIndex)
                trackIndexMap[audioTrackIndex] = muxer.addTrack(extractor.getTrackFormat(audioTrackIndex))
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                val sampleTrackIndex = extractor.sampleTrackIndex
                if (sampleTrackIndex == -1 || extractor.sampleTime >= endUs) {
                    break
                }

                if (trackIndexMap.containsKey(sampleTrackIndex)) {
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    bufferInfo.presentationTimeUs = extractor.sampleTime - startUs
                    bufferInfo.flags = extractor.sampleFlags

                    if (bufferInfo.size > 0) {
                        muxer.writeSampleData(trackIndexMap.getValue(sampleTrackIndex), buffer, bufferInfo)
                    }

                    val progress = (extractor.sampleTime - startUs).toFloat() / (endUs - startUs).toFloat() * 100
                    onProgress(progress)
                }
                extractor.advance()
            }
        } finally {
            extractor.release()
            muxer?.stop()
            muxer?.release()
        }
    }


    private fun createForegroundInfo(progress: String): ForegroundInfo {
        createNotificationChannel()
        val notification = createNotification(progress)
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        return ForegroundInfo(NOTIFICATION_ID, notification, foregroundServiceType)
    }

    private fun createNotification(progress: String) = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        .setContentTitle("Exporting Video")
        .setContentText(progress)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun showCompletionNotification() {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Export Complete")
            .setContentText("Video saved to Movies folder.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Export",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}