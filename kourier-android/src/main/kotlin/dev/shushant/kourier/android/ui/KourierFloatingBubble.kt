package dev.shushant.kourier.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.shushant.kourier.android.KourierStats
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.ui.triggers.FloatingDebugBubble

/**
 * Draggable floating bubble that shows live error / active-request counts.
 * Tap to open the Kourier debugger.
 *
 * Usage:
 *   val stats by KourierTelemetry.stats.collectAsState()
 *   KourierFloatingBubble(
 *       stats = stats,
 *       onClick = { Kourier.showUI() },
 *       modifier = Modifier.align(Alignment.TopStart)
 *   )
 */
@Composable
fun KourierFloatingBubble(
    stats: KourierStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Map public KourierStats → internal TelemetryStats for the underlying component
    FloatingDebugBubble(
        telemetry = TelemetryStats(
            totalRequests = stats.totalRequests,
            activeRequests = stats.activeRequests,
            errorCount = stats.errorCount,
            totalBytesSent = stats.totalBytesSent,
            totalBytesReceived = stats.totalBytesReceived
        ),
        onClick = onClick,
        modifier = modifier
    )
}
