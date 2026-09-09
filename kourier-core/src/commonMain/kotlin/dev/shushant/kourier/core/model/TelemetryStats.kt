package dev.shushant.kourier.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TelemetryStats(
    val totalRequests: Long = 0L,
    val activeRequests: Int = 0,
    val errorCount: Long = 0L,
    val totalBytesSent: Long = 0L,
    val totalBytesReceived: Long = 0L,
    val interceptorActive: Boolean = true
) {
    val formattedThroughput: String
        get() = "${formatBytes(totalBytesSent)} ↑ / ${formatBytes(totalBytesReceived)} ↓"

    companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${(bytes / (1024.0 * 1024.0) * 10).toLong() / 10.0} MB"
                else -> "${(bytes / (1024.0 * 1024.0 * 1024.0) * 10).toLong() / 10.0} GB"
            }
        }
    }
}
