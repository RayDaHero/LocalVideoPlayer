package com.example.localvideoplayer.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.example.localvideoplayer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class VideoExportWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_INPUT_URI = "KEY_INPUT_URI"
        const val KEY_START_MS = "KEY_START_MS"
        const val KEY_END_MS = "KEY_END_MS"
        const val KEY_OUTPUT_URI = "KEY_OUTPUT_URI"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "VideoExportChannel"
    }

    override suspend fun doWork(): Result {
        val inputUriString = inputData.getString(KEY_INPUT_URI) ?: return Result.failure()
        val startTimeMs = inputData.getLong(KEY_START_MS, 0)
        val endTimeMs = inputData.getLong(KEY_END_MS, 0)

        val safeInputUri = FFmpegKitConfig.getSafParameterForRead(applicationContext, Uri.parse(inputUriString))

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

                val safeOutputUri = FFmpegKitConfig.getSafParameterForWrite(applicationContext, outputUri)

                // CORRECTED: Added high-quality video and audio flags (-crf 18, -c:a aac)
                val command = "-y -i $safeInputUri -ss ${formatTime(startTimeMs)} -to ${formatTime(endTimeMs)} -c:v libx264 -crf 18 -c:a aac $safeOutputUri"

                FFmpegKitConfig.enableStatisticsCallback { stats ->
                    val duration = endTimeMs - startTimeMs
                    if (duration > 0) {
                        val progress = (stats.time.toFloat() / duration.toFloat()) * 100
                        val notification = createNotification("Exporting... ${progress.toInt()}%")
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                }

                val session = FFmpegKit.execute(command)

                if (ReturnCode.isSuccess(session.returnCode)) {
                    showCompletionNotification(outputUri)
                    Result.success(workDataOf(KEY_OUTPUT_URI to outputUri.toString()))
                } else {
                    Log.e("VideoExportWorker", "FFmpeg command failed with state ${session.state} and return code ${session.returnCode}. Log: ${session.allLogsAsString}")
                    resolver.delete(outputUri, null, null)
                    Result.failure()
                }

            } catch (e: Exception) {
                Log.e("VideoExportWorker", "Export failed with exception", e)
                Result.failure()
            }
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

    private fun showCompletionNotification(uri: Uri) {
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

    private fun formatTime(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
}