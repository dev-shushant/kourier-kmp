package dev.shushant.kourier

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.storage.SQLiteKourierStorage
import dev.shushant.kourier.storage.db.DatabaseDriverFactory
import dev.shushant.kourier.ui.KourierUI
import dev.shushant.kourier.ui.triggers.IosOverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual object Kourier {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Initialises Kourier. Call once at app startup. */
    fun initialize(config: KourierConfig = KourierConfig.Builder().build()) {
        val storage = SQLiteKourierStorage(DatabaseDriverFactory().createDriver())
        KourierCore.initialize(config = config, storage = storage)
        KourierUI.startTriggers()
    }

    /** Builder-DSL variant of [initialize]. */
    fun initialize(block: KourierConfig.Builder.() -> Unit) {
        initialize(KourierConfig.Builder().apply(block).build())
    }

    /** Shows the floating debug bubble. */
    fun showBubble() = IosOverlayController.install()

    /** Hides the floating debug bubble. */
    fun hideBubble() = IosOverlayController.uninstall()

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
