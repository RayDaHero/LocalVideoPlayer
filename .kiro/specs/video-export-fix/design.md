# Design Document

## Overview

This design addresses the compilation errors in PlayerViewModel.kt by implementing the missing video export functionality in the VideoRepository. The solution will provide a clean separation of concerns with proper error handling and background processing.

## Architecture

The video export functionality will be implemented using:
- **VideoRepository**: Core export logic and file operations
- **WorkManager**: Background processing for video export tasks
- **LiveData**: Communication between repository and ViewModel for export status
- **FFmpeg**: Video processing and clip extraction

## Components and Interfaces

### VideoRepository Export Methods

```kotlin
// Export event LiveData for status updates
val exportEvent: LiveData<ExportStatus>

// Main export method
fun exportVideoClip(uri: Uri, startTimeSeconds: Double, endTimeSeconds: Double)
```

### Export Status Data Class

```kotlin
sealed class ExportStatus {
    object Idle : ExportStatus()
    object InProgress : ExportStatus()
    data class Success(val outputPath: String) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}
```

### VideoExportWorker Integration

The existing VideoExportWorker will be integrated with the repository to handle the actual video processing using FFmpeg.

## Data Models

### Export Status Model
- **Idle**: No export operation in progress
- **InProgress**: Export is currently running
- **Success**: Export completed with output file path
- **Error**: Export failed with error message

## Error Handling

1. **File Access Errors**: Handle cases where input video cannot be read
2. **Storage Errors**: Handle insufficient storage space
3. **FFmpeg Errors**: Handle video processing failures
4. **Permission Errors**: Handle storage permission issues

## Testing Strategy

1. **Unit Tests**: Test export logic with mock video files
2. **Integration Tests**: Test WorkManager integration
3. **Error Scenarios**: Test various failure conditions
4. **Performance Tests**: Ensure export doesn't block UI thread