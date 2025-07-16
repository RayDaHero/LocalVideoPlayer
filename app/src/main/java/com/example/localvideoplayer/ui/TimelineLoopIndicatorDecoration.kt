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

        val startMs = viewModel.loopStartPointMs.value ?: return
        val endMs = viewModel.loopEndPointMs.value ?: return
        val thumbnails = viewModel.thumbnails.value ?: return

        var startX: Float? = null
        var endX: Float? = null

        for (i in 0 until parent.childCount) {
            val view = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) continue

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