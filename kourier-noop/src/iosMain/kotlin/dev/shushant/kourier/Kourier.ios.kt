package dev.shushant.kourier

import dev.shushant.kourier.core.config.KourierConfig

actual object Kourier {
    /** No-op for release builds. */
    fun initialize(config: KourierConfig = KourierConfig.Builder().build()) {}

    /** No-op for release builds. */
    fun initialize(block: KourierConfig.Builder.() -> Unit) {}

    fun showBubble() {}

    fun hideBubble() {}

    actual fun showUI() {}

    actual fun hideUI() {}

    actual fun observeStats(callback: (total: Long, active: Int, errors: Long) -> Unit): () -> Unit = {}
}
