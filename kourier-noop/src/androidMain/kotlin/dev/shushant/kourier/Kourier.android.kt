package dev.shushant.kourier

import android.content.Context
import dev.shushant.kourier.core.config.KourierConfig

actual object Kourier {
    /** No-op for release builds. */
    fun init(context: Context, block: KourierConfig.Builder.() -> Unit = {}) {}

    actual fun showUI() {}

    actual fun hideUI() {}

    actual fun observeStats(callback: (total: Long, active: Int, errors: Long) -> Unit): () -> Unit = {}
}
