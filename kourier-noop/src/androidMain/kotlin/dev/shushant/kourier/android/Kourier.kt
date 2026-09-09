package dev.shushant.kourier.android

import android.content.Context
import dev.shushant.kourier.core.config.KourierConfig

object Kourier {
    fun init(
        context: Context,
        config: KourierConfig = KourierConfig.Builder().build()
    ) {
        // No-op for release builds: zero overhead
    }

    fun init(
        context: Context,
        block: KourierConfig.Builder.() -> Unit
    ) {
        // No-op for release builds: zero overhead
    }

    fun showUI() {
        // No-op for release builds: zero overhead
    }

    fun hideUI() {
        // No-op for release builds: zero overhead
    }
}
