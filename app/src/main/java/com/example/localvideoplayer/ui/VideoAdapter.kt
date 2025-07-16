package com.example.localvideoplayer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.localvideoplayer.R
import com.example.localvideoplayer.data.VideoItem
import com.example.localvideoplayer.databinding.ItemVideoBinding

class VideoAdapter(private val onClick: (VideoItem) -> Unit) :
    ListAdapter<VideoItem, VideoAdapter.VideoViewHolder>(VideoDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getItem(position)
        holder.bind(video)
    }

    class VideoViewHolder(
        private val binding: ItemVideoBinding,
        private val onClick: (VideoItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentVideo: VideoItem? = null

        init {
            itemView.setOnClickListener {
                currentVideo?.let {
                    onClick(it)
                }
            }
        }

        fun bind(video: VideoItem) {
            currentVideo = video
            binding.videoTitle.text = video.name
            binding.videoDetails.text = "${video.duration} - ${video.resolution}"

            Glide.with(binding.videoThumbnail.context)
                .load(video.uri)
                .centerCrop()
                .error(R.drawable.ic_launcher_background) // Keep the placeholder
                .into(binding.videoThumbnail)
        }
    }
}

object VideoDiffCallback : DiffUtil.ItemCallback<VideoItem>() {
    override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
        return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
        return oldItem == newItem
    }
}