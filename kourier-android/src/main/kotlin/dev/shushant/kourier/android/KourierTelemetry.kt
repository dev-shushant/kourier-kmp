package dev.shushant.kourier.android

import dev.shushant.kourier.core.KourierCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Public telemetry data exposed to the host app.
 *
 * Mirrors the internal TelemetryStats without leaking SDK-internal types.
 */
data class KourierStats(
    val totalRequests: Long = 0L,
    val activeRequests: Int = 0,
    val errorCount: Long = 0L,
    val totalBytesSent: Long = 0L,
    val totalBytesReceived: Long = 0L
)

/**
 * Live telemetry from the Kourier SDK.
 *
 * Usage in Compose:
 *   val stats by Kourier.stats.collectAsState()
 */
object KourierTelemetry {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val stats: StateFlow<KourierStats> = KourierCore.eventBus.telemetry
        .map { t ->
            KourierStats(
                totalRequests = t.totalRequests,
                activeRequests = t.activeRequests,
                errorCount = t.errorCount,
                totalBytesSent = t.totalBytesSent,
                totalBytesReceived = t.totalBytesReceived
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = KourierStats()
        )
}
