package com.example.localvideoplayer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.localvideoplayer.data.ThumbnailItem
import com.example.localvideoplayer.databinding.ItemThumbnailBinding
import java.util.concurrent.TimeUnit

class ThumbnailAdapter(private val onThumbnailClick: (Long) -> Unit) :
    ListAdapter<ThumbnailItem, ThumbnailAdapter.ThumbnailViewHolder>(ThumbnailDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val binding = ItemThumbnailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThumbnailViewHolder(binding, onThumbnailClick)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ThumbnailViewHolder(
        private val binding: ItemThumbnailBinding,
        private val onThumbnailClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ThumbnailItem) {
            binding.thumbnailImage.setImageBitmap(item.bitmap)
            binding.root.setOnClickListener {
                onThumbnailClick(item.timestamp)
            }

            // --- ADDED: Format and display the timestamp ---
            val minutes = TimeUnit.MILLISECONDS.toMinutes(item.timestamp) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(item.timestamp) % 60
            binding.thumbnailTimestamp.text = String.format("%02d:%02d", minutes, seconds)
        }
    }
}

object ThumbnailDiffCallback : DiffUtil.ItemCallback<ThumbnailItem>() {
    override fun areItemsTheSame(oldItem: ThumbnailItem, newItem: ThumbnailItem): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: ThumbnailItem, newItem: ThumbnailItem): Boolean {
        // Bitmap comparison is expensive and not needed for DiffUtil if timestamp is unique
        return oldItem.timestamp == newItem.timestamp
    }
}