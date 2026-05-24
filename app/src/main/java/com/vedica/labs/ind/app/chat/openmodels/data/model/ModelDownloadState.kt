package com.vedica.labs.ind.app.chat.openmodels.data.model

data class ModelDownloadState(
    val modelId: String,
    val progressPercentage: Double = 0.0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val downloadSpeedMbps: Double = 0.0,
    val status: String = "DOWNLOADING",
    val error: String? = null
) {
    val isDownloading: Boolean get() = status == "DOWNLOADING"
    val isCompleted: Boolean get() = status == "COMPLETED"
    val isError: Boolean get() = status == "ERROR"
    val progressFraction: Float get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}
