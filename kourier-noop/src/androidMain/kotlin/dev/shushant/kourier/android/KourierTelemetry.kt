package dev.shushant.kourier.android

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class KourierStats(
    val totalRequests: Long = 0,
    val activeRequests: Long = 0,
    val errorCount: Long = 0,
    val totalBytesSent: Long = 0,
    val totalBytesReceived: Long = 0
)

object KourierTelemetry {
    private val _stats = MutableStateFlow(KourierStats())
    val stats: StateFlow<KourierStats> = _stats.asStateFlow()
}
