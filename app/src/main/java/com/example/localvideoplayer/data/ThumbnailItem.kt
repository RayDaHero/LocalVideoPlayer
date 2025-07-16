package com.example.localvideoplayer.data

import android.graphics.Bitmap

data class ThumbnailItem(
    val bitmap: Bitmap,
    val timestamp: Long // The timestamp in milliseconds this thumbnail represents
)