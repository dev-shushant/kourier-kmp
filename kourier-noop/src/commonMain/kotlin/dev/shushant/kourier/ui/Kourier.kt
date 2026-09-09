package dev.shushant.kourier.ui

import dev.shushant.kourier.core.config.KourierConfig

object Kourier {
    fun init(config: KourierConfig = KourierConfig()) {
        // No-op for release builds
    }

    fun init(block: KourierConfig.Builder.() -> Unit) {
        // No-op for release builds
    }

    fun showUI() {
        // No-op for release builds
    }

    fun hideUI() {
        // No-op for release builds
    }
}
