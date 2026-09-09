package dev.shushant.kourier.ios

import dev.shushant.kourier.core.config.KourierConfig
import kotlin.experimental.ExperimentalObjCName

@OptIn(ExperimentalObjCName::class)
object Kourier {
    @ObjCName("doInit")
    fun initialize(config: KourierConfig = KourierConfig.Builder().build()) {
        // No-op for release builds: zero overhead
    }

    @ObjCName("doInit")
    fun initialize(block: KourierConfig.Builder.() -> Unit) {
        // No-op for release builds: zero overhead
    }

    fun showUI() {
        // No-op for release builds: zero overhead
    }

    fun hideUI() {
        // No-op for release builds: zero overhead
    }

    fun showBubble() {
        // No-op for release builds: zero overhead
    }

    fun hideBubble() {
        // No-op for release builds: zero overhead
    }

    fun bubbleIconBase64(): String = ""

    fun observeStats(callback: (total: Long, active: Int, errors: Long) -> Unit): () -> Unit {
        return {}
    }
}
