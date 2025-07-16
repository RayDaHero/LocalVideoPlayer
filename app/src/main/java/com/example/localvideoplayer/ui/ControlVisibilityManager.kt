package com.example.localvideoplayer.ui

import android.os.Handler
import android.os.Looper
import androidx.media3.ui.PlayerView

/**
 * Manages the visibility of video player controls with context-aware behavior
 */
class ControlVisibilityManager(
    private val playerView: PlayerView,
    private val activity: PlayerActivity
) {
    private val handler = Handler(Looper.getMainLooper())
    private var hideControlsRunnable: Runnable? = null
    private var currentConfig = ControlVisibilityConfig()
    private var currentState = ControlVisibilityState.AUTO_HIDE

    /**
     * Sets whether controls should always be visible
     */
    fun setControlsAlwaysVisible(alwaysVisible: Boolean) {
        currentState = if (alwaysVisible) {
            ControlVisibilityState.ALWAYS_VISIBLE
        } else {
            ControlVisibilityState.AUTO_HIDE
        }

        if (alwaysVisible) {
            playerView.controllerShowTimeoutMs = 0
            cancelHideTimer()
            showControls()
        } else {
            playerView.controllerShowTimeoutMs = currentConfig.timeoutMs.toInt()
            resetControlsTimer()
        }
    }

    /**
     * Resets the control visibility timer
     */
    fun resetControlsTimer() {
        if (currentState == ControlVisibilityState.ALWAYS_VISIBLE || currentState == ControlVisibilityState.CUSTOM_MODE) {
            return
        }

        cancelHideTimer()
        showControls()

        hideControlsRunnable = Runnable {
            hideControls()
        }
        handler.postDelayed(hideControlsRunnable!!, currentConfig.timeoutMs)
    }

    /**
     * Shows the controls immediately with smooth animation
     */
    fun showControls() {
        try {
            playerView.showController()
        } catch (e: Exception) {
            // Fallback: try to recover control state
            recoverControlState()
        }
    }

    /**
     * Hides the controls with smooth fade animation
     */
    fun hideControls() {
        try {
            // Only hide controls if in auto-hide mode
            if (currentState == ControlVisibilityState.AUTO_HIDE) {
                playerView.hideController()
            }
        } catch (e: Exception) {
            // Fallback: try to recover control state
            recoverControlState()
        }
    }

    /**
     * Recovers from control visibility errors
     */
    private fun recoverControlState() {
        try {
            // Reset to a known good state
            currentState = ControlVisibilityState.AUTO_HIDE
            cancelHideTimer()

            // Try to show controls as fallback
            playerView.showController()

            // Restart normal auto-hide behavior
            resetControlsTimer()
        } catch (e: Exception) {
            // If recovery fails, log and continue with degraded functionality
        }
    }

    /**
     * Sets custom controls mode (keeps video controls visible when custom controls are active)
     */
    fun setCustomControlsMode(isCustomMode: Boolean) {
        currentState = if (isCustomMode) {
            ControlVisibilityState.CUSTOM_MODE
        } else {
            ControlVisibilityState.AUTO_HIDE
        }

        if (isCustomMode) {
            // Disable the controller's internal timeout to keep it visible
            playerView.controllerShowTimeoutMs = 0
            cancelHideTimer()
            showControls()
        } else {
            // Restore the controller's internal timeout
            playerView.controllerShowTimeoutMs = currentConfig.timeoutMs.toInt()
            resetControlsTimer()
        }
    }

    /**
     * Called when user interacts with the player
     */
    fun onUserInteraction() {
        // Always show the controls on any interaction, then reset the timer
        showControls()
        resetControlsTimer()
    }

    /**
     * Updates the timeout configuration
     */
    fun updateConfig(config: ControlVisibilityConfig) {
        currentConfig = config
        if (currentState == ControlVisibilityState.AUTO_HIDE) {
            resetControlsTimer()
        }
    }

    /**
     * Cancels the current hide timer
     */
    private fun cancelHideTimer() {
        hideControlsRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            hideControlsRunnable = null
        }
    }

    /**
     * Cleanup method to be called when the manager is no longer needed
     */
    fun cleanup() {
        cancelHideTimer()
    }
}

/**
 * Enum representing different control visibility states
 */
enum class ControlVisibilityState {
    AUTO_HIDE,      // Normal auto-hide behavior
    ALWAYS_VISIBLE, // Controls always visible
    CUSTOM_MODE     // Custom controls are active
}

/**
 * Configuration for control visibility behavior
 */
data class ControlVisibilityConfig(
    val timeoutMs: Long = 10000L,
    val state: ControlVisibilityState = ControlVisibilityState.AUTO_HIDE,
    val fadeAnimationDuration: Long = 300L
)