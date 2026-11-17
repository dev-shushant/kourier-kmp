package dev.shushant.kourier

import android.content.Context
import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.ui.KourierUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual object Kourier {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Initialises Kourier. Call once from Application.onCreate(). */
    fun init(context: Context, block: KourierConfig.Builder.() -> Unit = {}) {
        dev.shushant.kourier.android.Kourier.init(context, block)
    }

    actual fun showUI() = KourierUI.show()

    actual fun hideUI() = KourierUI.hide()

    actual fun observeStats(callback: (total: Long, active: Int, errors: Long) -> Unit): () -> Unit {
        val job = scope.launch {
            KourierCore.eventBus.telemetry.collect { stats ->
                callback(stats.totalRequests, stats.activeRequests, stats.errorCount)
            }
        }
        return { job.cancel() }
    }
}
