package com.example.localvideoplayer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.localvideoplayer.data.ThumbnailItem
import com.example.localvideoplayer.databinding.ItemThumbnailBinding

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
        }
    }
}

object ThumbnailDiffCallback : DiffUtil.ItemCallback<ThumbnailItem>() {
    override fun areItemsTheSame(oldItem: ThumbnailItem, newItem: ThumbnailItem): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: ThumbnailItem, newItem: ThumbnailItem): Boolean {
        return oldItem.bitmap == newItem.bitmap
    }
}
