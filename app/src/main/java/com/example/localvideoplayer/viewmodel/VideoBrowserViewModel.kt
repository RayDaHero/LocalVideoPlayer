package com.example.localvideoplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.localvideoplayer.data.VideoItem
import com.example.localvideoplayer.repository.VideoRepository
import kotlinx.coroutines.launch

class VideoBrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)

    private val _allVideos = MutableLiveData<List<VideoItem>>()
    val allVideos: LiveData<List<VideoItem>> = _allVideos

    private val _exportedVideos = MutableLiveData<List<VideoItem>>()
    val exportedVideos: LiveData<List<VideoItem>> = _exportedVideos

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadAllVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _allVideos.postValue(repository.getVideos(onlyExported = false))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadExportedVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _exportedVideos.postValue(repository.getVideos(onlyExported = true))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
