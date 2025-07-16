package com.example.localvideoplayer.data

/**
 * Sealed class representing the status of video export operations
 */
sealed class ExportStatus {
    /**
     * No export operation is currently in progress
     */
    object Idle : ExportStatus()
    
    /**
     * Export operation is currently running
     */
    object InProgress : ExportStatus()
    
    /**
     * Export completed successfully
     * @param outputPath The file path of the exported video clip
     */
    data class Success(val outputPath: String) : ExportStatus()
    
    /**
     * Export failed with an error
     * @param message Error message describing what went wrong
     */
    data class Error(val message: String) : ExportStatus()
}