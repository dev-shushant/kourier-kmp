package dev.shushant.kourier.core.model

import kotlinx.serialization.Serializable

@Serializable
data class HttpTimings(
    val requestStartNs: Long = 0L,
    val requestEndNs: Long = 0L,
    val responseStartNs: Long = 0L,
    val responseEndNs: Long = 0L,
    val durationMs: Long = 0L,
    val timeToFirstByteMs: Long = 0L
) {
    val requestDurationMs: Long
        get() = if (requestEndNs > requestStartNs) (requestEndNs - requestStartNs) / 1_000_000L else 0L

    val responseDurationMs: Long
        get() = if (responseEndNs > responseStartNs) (responseEndNs - responseStartNs) / 1_000_000L else 0L

    val calculatedTotalMs: Long
        get() = if (durationMs > 0) durationMs else {
            if (responseEndNs > requestStartNs && requestStartNs > 0) {
                (responseEndNs - requestStartNs) / 1_000_000L
            } else 0L
        }
}
