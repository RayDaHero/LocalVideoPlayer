package com.example.localvideoplayer.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.localvideoplayer.data.ThumbnailItem
import com.example.localvideoplayer.repository.VideoRepository
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)
    private val _thumbnails = MutableLiveData<List<ThumbnailItem>>(emptyList())
    val thumbnails: LiveData<List<ThumbnailItem>> = _thumbnails

    private val _isGeneratingThumbnails = MutableLiveData<Boolean>()
    val isGeneratingThumbnails: LiveData<Boolean> = _isGeneratingThumbnails

    // NEW: LiveData for loop points
    private val _loopStartPointMs = MutableLiveData<Long?>()
    val loopStartPointMs: LiveData<Long?> = _loopStartPointMs

    private val _loopEndPointMs = MutableLiveData<Long?>()
    val loopEndPointMs: LiveData<Long?> = _loopEndPointMs

    fun setLoopStartPoint(timeMs: Long) {
        // Ensure start is before end
        if (_loopEndPointMs.value == null || timeMs < _loopEndPointMs.value!!) {
            _loopStartPointMs.value = timeMs
        }
    }

    fun setLoopEndPoint(timeMs: Long) {
        // Ensure end is after start
        if (_loopStartPointMs.value == null || timeMs > _loopStartPointMs.value!!) {
            _loopEndPointMs.value = timeMs
        }
    }

    fun clearLoopPoints() {
        _loopStartPointMs.value = null
        _loopEndPointMs.value = null
    }

    fun generateThumbnails(videoUri: Uri) {
        viewModelScope.launch {
            _isGeneratingThumbnails.value = true
            try {
                repository.generateThumbnails(videoUri, interval = 5).collect { thumbnail ->
                    val currentThumbnails = _thumbnails.value?.toMutableList() ?: mutableListOf()
                    currentThumbnails.add(thumbnail)
                    _thumbnails.postValue(currentThumbnails)
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isGeneratingThumbnails.value = false
            }
        }
    }
}