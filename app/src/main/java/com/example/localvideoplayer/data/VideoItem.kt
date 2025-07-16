package com.example.localvideoplayer.data

import android.net.Uri

data class VideoItem(
    val uri: Uri,
    val name: String,
    val duration: String, // Formatted as HH:MM:SS or MM:SS
    val size: Long, // Size in bytes
    val resolution: String // Formatted as "Width x Height"
)