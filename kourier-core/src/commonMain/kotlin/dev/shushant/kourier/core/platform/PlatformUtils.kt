package dev.shushant.kourier.core.platform

expect object PlatformUtils {
    /** Generates a RFC-4122 compliant UUID v4 string. */
    fun randomUuid(): String

    /** Current monotonic timestamp in nanoseconds for high precision timing. */
    fun nanoTime(): Long

    /** Current wall-clock time in epoch milliseconds. */
    fun currentTimeMillis(): Long

    /** Returns human-readable platform description (e.g., "Android 14 (API 34)" or "iOS 17.5"). */
    fun platformName(): String

    /** Captures the current thread's call stack, omitting framework/internal frames. */
    fun captureSanitizedCallStack(maxDepth: Int = 15): List<dev.shushant.kourier.core.model.CallStackElement>
}
