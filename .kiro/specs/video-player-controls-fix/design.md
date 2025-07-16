# Design Document

## Overview

This design addresses the video player controls disappearing issue by implementing a comprehensive control visibility management system. The solution involves extending the timeout duration, adding intelligent visibility management based on user context, optimizing memory operations to prevent UI interference, and ensuring proper coordination between ExoPlayer controls and custom overlay controls.

## Architecture

### Control Visibility Management System
- **ControlVisibilityManager**: A dedicated class to manage when controls should be visible, hidden, or always visible
- **User Interaction Detection**: Touch event listeners to detect user activity and reset timers
- **Context-Aware Visibility**: Different visibility behaviors based on app state (custom controls open, loop setting mode, etc.)
- **Memory-Safe Operations**: Ensure memory cleanup doesn't interfere with UI responsiveness

### Component Integration
- **ExoPlayer Integration**: Proper configuration of PlayerControlView timeout and visibility settings
- **Custom Controls Coordination**: Ensure custom overlay controls work harmoniously with video controls
- **Layout Optimization**: Prevent touch event conflicts between overlapping controls

## Components and Interfaces

### 1. ControlVisibilityManager Class
```kotlin
class ControlVisibilityManager(
    private val playerView: PlayerView,
    private val activity: PlayerActivity
) {
    fun setControlsAlwaysVisible(alwaysVisible: Boolean)
    fun resetControlsTimer()
    fun showControls()
    fun hideControls()
    fun setCustomControlsMode(isCustomMode: Boolean)
}
```

### 2. Enhanced PlayerActivity Methods
```kotlin
// Control visibility management
private fun setupControlVisibilityManager()
private fun onUserInteraction()
private fun updateControlsVisibilityForCustomPanel(isVisible: Boolean)

// Memory-safe operations
private fun performMemoryCleanupSafely()
private fun optimizeMemoryOperations()
```

### 3. Layout Modifications
- Increase default timeout from 5000ms to 10000ms (10 seconds)
- Add proper touch event handling to prevent conflicts
- Optimize control positioning to avoid overlaps

### 4. User Interaction Detection
- Touch event listeners on video area
- Control interaction callbacks
- Custom control panel state monitoring

## Data Models

### ControlVisibilityState
```kotlin
enum class ControlVisibilityState {
    AUTO_HIDE,      // Normal auto-hide behavior
    ALWAYS_VISIBLE, // Controls always visible
    CUSTOM_MODE     // Custom controls are active
}

data class ControlVisibilityConfig(
    val timeoutMs: Long = 10000L,
    val state: ControlVisibilityState = ControlVisibilityState.AUTO_HIDE,
    val fadeAnimationDuration: Long = 300L
)
```

## Error Handling

### Memory Pressure Handling
- Defer non-critical memory operations when controls are active
- Use background threads for memory cleanup to avoid UI blocking
- Implement graceful degradation if memory is critically low

### Touch Event Conflicts
- Proper event propagation between overlapping controls
- Fallback mechanisms if touch events are not properly handled
- Error recovery for unresponsive control states

### ExoPlayer State Management
- Handle player state changes that might affect control visibility
- Recover from ExoPlayer internal errors that could hide controls
- Maintain control state consistency across player lifecycle events

## Testing Strategy

### Unit Tests
- ControlVisibilityManager behavior under different states
- Memory cleanup operations don't interfere with UI updates
- Touch event handling and timer reset functionality
- Control state transitions and edge cases

### Integration Tests
- ExoPlayer control integration with custom visibility management
- Custom controls panel interaction with video controls
- Memory pressure scenarios and control responsiveness
- User interaction detection and timer reset behavior

### UI Tests
- Control visibility timeout behavior (10-second default)
- Smooth fade animations and transitions
- Touch event handling across different control areas
- Custom controls panel opening/closing with proper control coordination

### Performance Tests
- Memory cleanup operations don't cause UI lag
- Control visibility changes are smooth and responsive
- Background operations don't interfere with user interactions
- App performance under various memory conditions

## Implementation Approach

### Phase 1: Basic Timeout Fix
1. Update custom_player_control.xml timeout to 10 seconds
2. Add basic control visibility management methods
3. Implement user interaction detection

### Phase 2: Advanced Visibility Management
1. Create ControlVisibilityManager class
2. Implement context-aware visibility (custom controls mode)
3. Add proper touch event handling

### Phase 3: Memory Operation Optimization
1. Make memory cleanup operations non-blocking
2. Implement safe memory management that doesn't interfere with UI
3. Add performance monitoring for control responsiveness

### Phase 4: Integration and Polish
1. Ensure smooth coordination between all control types
2. Add fade animations and visual polish
3. Comprehensive testing and edge case handling