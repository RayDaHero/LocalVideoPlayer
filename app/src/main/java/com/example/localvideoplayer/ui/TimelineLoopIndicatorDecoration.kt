package com.example.localvideoplayer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.recyclerview.widget.RecyclerView
import com.example.localvideoplayer.viewmodel.PlayerViewModel

class TimelineLoopIndicatorDecoration(
    context: Context,
    private val viewModel: PlayerViewModel
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        color = Color.parseColor("#80FFEB3B") // Semi-transparent yellow
        style = Paint.Style.FILL
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        // Get loop position from the updated ViewModel structure
        val loopPosition = viewModel.loopPosition.value ?: return
        val startMs = loopPosition.first
        val endMs = loopPosition.second
        
        // Check if both start and end are valid (not -1L)
        if (startMs == -1L || endMs == -1L) return
        
        // Get thumbnails from Resource wrapper
        val thumbnailsResource = viewModel.thumbnails.value ?: return
        val thumbnails = when (thumbnailsResource) {
            is com.example.localvideoplayer.data.Resource.Success -> thumbnailsResource.data ?: return
            else -> return // Don't draw if loading or error
        }

        var startX: Float? = null
        var endX: Float? = null

        for (i in 0 until parent.childCount) {
            val view = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION || position >= thumbnails.size) continue

            val item = thumbnails[position]

            if (item.timestamp >= startMs && startX == null) {
                startX = view.left.toFloat()
            }

            if (item.timestamp <= endMs) {
                endX = view.right.toFloat()
            }
        }

        if (startX != null && endX != null) {
            val rect = RectF(startX, parent.paddingTop.toFloat(), endX, (parent.height - parent.paddingBottom).toFloat())
            c.drawRect(rect, paint)
        }
    }
}