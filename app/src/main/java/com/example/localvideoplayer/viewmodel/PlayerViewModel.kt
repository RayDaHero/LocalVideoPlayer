package com.example.localvideoplayer.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.localvideoplayer.data.Resource
import com.example.localvideoplayer.data.ThumbnailItem
import com.example.localvideoplayer.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val videoRepository = VideoRepository(application)

    // LiveData for the list of thumbnails
    private val _thumbnails = MutableLiveData<Resource<List<ThumbnailItem>>>()
    val thumbnails: LiveData<Resource<List<ThumbnailItem>>> = _thumbnails

    // LiveData for the loop selection
    private val _loopPosition = MutableLiveData<Pair<Long, Long>?>(null)
    val loopPosition: LiveData<Pair<Long, Long>?> = _loopPosition

    // LiveData for video export events
    val exportEvent = videoRepository.exportEvent

    /**
     * Generates video thumbnails efficiently with reduced memory usage and better performance.
     */
    fun generateThumbnails(uri: Uri) {
        viewModelScope.launch {
            _thumbnails.value = Resource.Loading()

            try {
                val thumbnailList = withContext(Dispatchers.IO) {
                    val thumbnails = mutableListOf<ThumbnailItem>()
                    val retriever = MediaMetadataRetriever()

                    retriever.use {
                        it.setDataSource(getApplication<Application>(), uri)
                        val durationMs = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0

                        val intervalMs = 5000L
                        val maxThumbnails = 40
                        val actualInterval = maxOf(intervalMs, durationMs / maxThumbnails)

                        var timeMs = 0L
                        while (timeMs < durationMs && thumbnails.size < maxThumbnails) {
                            try {
                                val bitmap = it.getFrameAtTime(
                                    timeMs * 1000,
                                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                                )

                                if (bitmap != null) {
                                    val scaledBitmap = Bitmap.createScaledBitmap(
                                        bitmap,
                                        160,
                                        (160 * bitmap.height) / bitmap.width,
                                        true
                                    )
                                    bitmap.recycle()
                                    thumbnails.add(ThumbnailItem(scaledBitmap, timeMs))

                                    if (thumbnails.size % 5 == 0) {
                                        System.gc()
                                    }
                                }
                            } catch (e: Exception) {
                                continue
                            }
                            timeMs += actualInterval
                        }
                    }
                    thumbnails
                }
                _thumbnails.value = Resource.Success(thumbnailList)
            } catch (e: Exception) {
                _thumbnails.value = Resource.Error("Failed to generate thumbnails: ${e.message}")
            }
        }
    }

    /**
     * Sets the start of the loop position.
     */
    fun setLoopStart(timeMs: Long) {
        val end = _loopPosition.value?.second ?: -1L
        _loopPosition.value = Pair(timeMs, end)
    }

    /**
     * Sets the end of the loop position.
     */
    fun setLoopEnd(timeMs: Long) {
        val start = _loopPosition.value?.first ?: -1L
        if (timeMs > start) {
            _loopPosition.value = Pair(start, timeMs)
        }
    }

    /**
     * Resets the loop selection.
     */
    fun clearLoop() {
        _loopPosition.value = null
    }

    /**
     * Starts the video export process for the selected loop range.
     */
    fun exportVideo(uri: Uri) {
        val loop = _loopPosition.value
        if (loop != null && loop.first != -1L && loop.second != -1L) {
            // MODIFIED: Pass milliseconds directly to the repository
            videoRepository.exportVideoClip(uri, loop.first, loop.second)
        }
    }
}