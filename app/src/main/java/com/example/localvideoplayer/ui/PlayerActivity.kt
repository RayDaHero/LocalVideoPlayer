package com.example.localvideoplayer.ui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localvideoplayer.R
import com.example.localvideoplayer.data.ExportStatus
import com.example.localvideoplayer.data.Resource
import com.example.localvideoplayer.databinding.ActivityPlayerBinding
import com.example.localvideoplayer.viewmodel.PlayerViewModel
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

    private lateinit var controlVisibilityManager: ControlVisibilityManager
    
    // Scrubber properties
    private var isUserSeeking = false
    private var videoDurationMs = 0L
    private val scrubberUpdateHandler = Handler(Looper.getMainLooper())
    private val scrubberUpdateRunnable = object : Runnable {
        override fun run() {
            updateScrubber()
            scrubberUpdateHandler.postDelayed(this, 1000) // Update every second
        }
    }

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
        setupControlVisibilityManager()
        setupVideoScrubber()
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
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.playWhenReady = true
            player.prepare()

            playerListener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        loopRunnable?.let { loopHandler.post(it) }
                        scrubberUpdateHandler.post(scrubberUpdateRunnable)
                    } else {
                        loopRunnable?.let { loopHandler.removeCallbacks(it) }
                        scrubberUpdateHandler.removeCallbacks(scrubberUpdateRunnable)
                    }
                    updatePlayPauseButton()
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                        handleUserInteraction()
                    }
                }
            }
            player.addListener(playerListener!!)
            updatePlayPauseButton()
        }
    }

    private fun updateLoopingState() {
        val player = exoPlayer ?: return
        val loopPair = viewModel.loopPosition.value
        val startMs = loopPair?.first
        val endMs = loopPair?.second

        loopRunnable?.let { loopHandler.removeCallbacks(it) }

        if (startMs != null && endMs != null && startMs != -1L && endMs != -1L) {
            player.repeatMode = Player.REPEAT_MODE_OFF

            loopRunnable = Runnable {
                if (player.currentPosition >= endMs) {
                    player.seekTo(startMs)
                }
                loopRunnable?.let { loopHandler.postDelayed(it, 100) }
            }

            if (player.isPlaying) {
                loopRunnable?.let { loopHandler.post(it) }
            }
        } else {
            player.repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    private fun setupClickListeners() {
        binding.setStartButton.setOnClickListener {
            exoPlayer?.currentPosition?.let { viewModel.setLoopStart(it) }
            handleUserInteraction()
        }
        binding.setEndButton.setOnClickListener {
            exoPlayer?.currentPosition?.let { viewModel.setLoopEnd(it) }
            handleUserInteraction()
        }
        binding.clearLoopButton.setOnClickListener {
            viewModel.clearLoop()
            handleUserInteraction()
        }
        binding.exportButton.setOnClickListener {
            if (videoUri != null) {
                viewModel.exportVideo(videoUri!!)
            }
            handleUserInteraction()
        }
        binding.toggleCustomControlsButton.setOnClickListener {
            toggleCustomControlsPanel()
            handleUserInteraction()
        }
        binding.playPauseButton.setOnClickListener {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }
            handleUserInteraction()
        }
    }

    private fun updatePlayPauseButton() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                binding.playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                binding.playPauseButton.setImageResource(android.R.drawable.ic_media_play)
            }
        }
    }

    private fun toggleCustomControlsPanel() {
        if (binding.customControlsPanel.visibility == View.GONE) {
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            binding.customControlsPanel.startAnimation(slideUp)
            binding.customControlsPanel.visibility = View.VISIBLE
            binding.toggleCustomControlsButton.setImageResource(R.drawable.ic_arrow_down)
            updateControlsVisibilityForCustomPanel(true)
        } else {
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            binding.customControlsPanel.startAnimation(slideDown)
            binding.customControlsPanel.visibility = View.GONE
            binding.toggleCustomControlsButton.setImageResource(R.drawable.ic_arrow_up)
            updateControlsVisibilityForCustomPanel(false)
        }
    }

    private fun updateControlsVisibilityForCustomPanel(isVisible: Boolean) {
        if (::controlVisibilityManager.isInitialized) {
            controlVisibilityManager.setCustomControlsMode(isVisible)
        }
    }

    private fun observeViewModel() {
        viewModel.thumbnails.observe(this, Observer { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.timelineProgressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.timelineProgressBar.visibility = View.GONE
                    resource.data?.let { thumbnailAdapter.submitList(it) }
                }
                is Resource.Error -> {
                    binding.timelineProgressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        })

        viewModel.loopPosition.observe(this) {
            binding.thumbnailsRecyclerView.invalidateItemDecorations()
            updateLoopingState()
            updateLoopUi()
        }

        // --- MODIFIED: Added message for successful export ---
        viewModel.exportEvent.observe(this) { status ->
            when (status) {
                is ExportStatus.InProgress -> {
                    Toast.makeText(this, "Export started...", Toast.LENGTH_SHORT).show()
                }
                is ExportStatus.Success -> {
                    Toast.makeText(this, "Export successful!", Toast.LENGTH_SHORT).show()
                }
                is ExportStatus.Error -> {
                    Toast.makeText(this, "Export failed: ${status.message}", Toast.LENGTH_LONG).show()
                }
                else -> { // Idle
                }
            }
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

    private fun formatTime(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun releasePlayer() {
        loopRunnable?.let { loopHandler.removeCallbacks(it) }
        scrubberUpdateHandler.removeCallbacks(scrubberUpdateRunnable)
        playerListener?.let { exoPlayer?.removeListener(it) }
        exoPlayer?.release()
        exoPlayer = null
        cleanupThumbnails()
    }

    private fun cleanupThumbnails() {
        Thread {
            viewModel.thumbnails.value?.let { resource ->
                if (resource is Resource.Success) {
                    resource.data?.forEach { thumbnail ->
                        if (!thumbnail.bitmap.isRecycled) {
                            thumbnail.bitmap.recycle()
                        }
                    }
                }
            }
        }.start()
    }

    private fun setupThumbnailRecyclerView() {
        thumbnailAdapter = ThumbnailAdapter { timestamp ->
            exoPlayer?.seekTo(timestamp)
            handleUserInteraction()
        }
        binding.thumbnailsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PlayerActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = thumbnailAdapter
            addItemDecoration(TimelineLoopIndicatorDecoration(context, viewModel))
        }
    }

    private fun setupControlVisibilityManager() {
        controlVisibilityManager = ControlVisibilityManager(binding.playerView, this)
        binding.playerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                handleUserInteraction()
            }
            false
        }
    }

    private fun handleUserInteraction() {
        if (::controlVisibilityManager.isInitialized) {
            controlVisibilityManager.onUserInteraction()
        }
    }

    private fun setupVideoScrubber() {
        binding.videoScrubber.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val seekPosition = (progress / 100f * videoDurationMs).toLong()
                    binding.currentTimeText.text = formatTime(seekPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                handleUserInteraction()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 0
                val seekPosition = (progress / 100f * videoDurationMs).toLong()
                exoPlayer?.seekTo(seekPosition)
                isUserSeeking = false
                handleUserInteraction()
            }
        })
    }

    private fun updateScrubber() {
        if (!isUserSeeking && exoPlayer != null) {
            val currentPosition = exoPlayer!!.currentPosition
            val duration = exoPlayer!!.duration
            
            if (duration > 0) {
                val progress = ((currentPosition.toFloat() / duration) * 100).toInt()
                binding.videoScrubber.progress = progress
                binding.currentTimeText.text = formatTime(currentPosition)
                binding.totalTimeText.text = formatTime(duration)
                videoDurationMs = duration
            }
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
        cleanupThumbnails()
    }

    public override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        cleanupThumbnails()
        if (::controlVisibilityManager.isInitialized) {
            controlVisibilityManager.cleanup()
        }
    }
}