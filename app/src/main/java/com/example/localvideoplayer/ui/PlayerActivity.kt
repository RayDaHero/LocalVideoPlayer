// =================================================================================
// File: ui/PlayerActivity.kt
// Description: MODIFIED - Replaced looping logic with a stable, non-blocking implementation to fix UI and export issues.
// =================================================================================
package com.example.localvideoplayer.ui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.localvideoplayer.R
import com.example.localvideoplayer.databinding.ActivityPlayerBinding
import com.example.localvideoplayer.viewmodel.PlayerViewModel
import com.example.localvideoplayer.workers.VideoExportWorker
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()
    private var exoPlayer: ExoPlayer? = null
    private lateinit var thumbnailAdapter: ThumbnailAdapter
    private var videoUri: Uri? = null

    private val loopHandler = Handler(Looper.getMainLooper())
    private var loopRunnable: Runnable? = null
    private var playerListener: Player.Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoUri = intent.data
        if (videoUri == null) {
            finish()
            return
        }

        setupThumbnailRecyclerView()
        setupClickListeners()
        observeViewModel()

        binding.customControlsPanel.visibility = View.GONE
        viewModel.generateThumbnails(videoUri!!)
    }

    private fun initializePlayer() {
        if (videoUri == null) return

        exoPlayer = ExoPlayer.Builder(this).build().also { player ->
            binding.playerView.player = player
            val mediaItem = MediaItem.fromUri(videoUri!!)
            player.setMediaItem(mediaItem)
            player.repeatMode = Player.REPEAT_MODE_ONE // Loop full video by default
            player.playWhenReady = true
            player.prepare()
        }
    }

    private fun updateLoopingState() {
        val player = exoPlayer ?: return
        val loopPair = viewModel.loopPosition.value
        val startMs = loopPair?.first
        val endMs = loopPair?.second

        // Always clean up previous listeners and handlers to prevent memory leaks and redundant checks
        playerListener?.let { player.removeListener(it) }
        loopRunnable?.let { loopHandler.removeCallbacks(it) }

        if (startMs != null && endMs != null && startMs != -1L && endMs != -1L) {
            // A custom loop is set. Disable ExoPlayer's default looping.
            player.repeatMode = Player.REPEAT_MODE_OFF

            loopRunnable = Runnable {
                // This check is very lightweight and will not block the UI thread.
                if (player.currentPosition >= endMs) {
                    player.seekTo(startMs)
                }
                // Continue checking only if the runnable is not null
                loopRunnable?.let { loopHandler.postDelayed(it, 100) }
            }

            // We only need to know when the player starts/stops to manage the handler.
            playerListener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        loopRunnable?.let { loopHandler.post(it) }
                    } else {
                        loopRunnable?.let { loopHandler.removeCallbacks(it) }
                    }
                }
            }
            player.addListener(playerListener!!)

            // If the player is already playing when the loop is set, start the handler.
            if (player.isPlaying) {
                loopRunnable?.let { loopHandler.post(it) }
            }
        } else {
            // No custom loop, so ensure default looping is enabled.
            player.repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    private fun setupClickListeners() {
        binding.setStartButton.setOnClickListener {
            exoPlayer?.currentPosition?.let { viewModel.setLoopStart(it) }
        }
        binding.setEndButton.setOnClickListener {
            exoPlayer?.currentPosition?.let { viewModel.setLoopEnd(it) }
        }
        binding.clearLoopButton.setOnClickListener {
            viewModel.clearLoop()
        }
        binding.exportButton.setOnClickListener {
            if (videoUri != null) {
                viewModel.exportVideo(videoUri!!)
            }
        }

        binding.toggleCustomControlsButton.setOnClickListener {
            toggleCustomControlsPanel()
        }
    }

    private fun toggleCustomControlsPanel() {
        if (binding.customControlsPanel.visibility == View.GONE) {
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            binding.customControlsPanel.startAnimation(slideUp)
            binding.customControlsPanel.visibility = View.VISIBLE
            binding.toggleCustomControlsButton.setImageResource(R.drawable.ic_arrow_down)
        } else {
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            binding.customControlsPanel.startAnimation(slideDown)
            binding.customControlsPanel.visibility = View.GONE
            binding.toggleCustomControlsButton.setImageResource(R.drawable.ic_arrow_up)
        }
    }

    private fun observeViewModel() {
        viewModel.thumbnails.observe(this, Observer { resource ->
            when (resource) {
                is com.example.localvideoplayer.data.Resource.Loading -> {
                    binding.timelineProgressBar.visibility = View.VISIBLE
                }
                is com.example.localvideoplayer.data.Resource.Success -> {
                    binding.timelineProgressBar.visibility = View.GONE
                    resource.data?.let { thumbnailAdapter.submitList(it) }
                }
                is com.example.localvideoplayer.data.Resource.Error -> {
                    binding.timelineProgressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        })

        viewModel.loopPosition.observe(this) { loopPair ->
            binding.thumbnailsRecyclerView.invalidateItemDecorations()
            updateLoopingState()
            updateLoopUi()
        }
    }

    private fun updateLoopUi() {
        val loopPair = viewModel.loopPosition.value
        val startMs = loopPair?.first
        val endMs = loopPair?.second

        binding.loopStartText.text = if (startMs != null && startMs != -1L) "Start: ${formatTime(startMs)}" else "Start: -"
        binding.loopEndText.text = if (endMs != null && endMs != -1L) "End: ${formatTime(endMs)}" else "End: -"

        binding.exportButton.visibility = if (startMs != null && endMs != null && startMs != -1L && endMs != -1L) View.VISIBLE else View.GONE
    }

    private fun startExport(uri: Uri, startMs: Long, endMs: Long) {
        Toast.makeText(this, "Starting export...", Toast.LENGTH_SHORT).show()
        val workRequest = OneTimeWorkRequestBuilder<VideoExportWorker>()
            .setInputData(workDataOf(
                VideoExportWorker.KEY_INPUT_URI to uri.toString(),
                VideoExportWorker.KEY_START_MS to startMs,
                VideoExportWorker.KEY_END_MS to endMs
            ))
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun formatTime(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun releasePlayer() {
        loopRunnable?.let { loopHandler.removeCallbacks(it) }
        playerListener?.let { exoPlayer?.removeListener(it) }
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun setupThumbnailRecyclerView() {
        thumbnailAdapter = ThumbnailAdapter { timestamp ->
            exoPlayer?.seekTo(timestamp)
        }
        binding.thumbnailsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PlayerActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = thumbnailAdapter
            addItemDecoration(TimelineLoopIndicatorDecoration(context, viewModel))
        }
    }

    public override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        if ((Build.VERSION.SDK_INT <= 23 || exoPlayer == null)) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            releasePlayer()
        }
    }
}