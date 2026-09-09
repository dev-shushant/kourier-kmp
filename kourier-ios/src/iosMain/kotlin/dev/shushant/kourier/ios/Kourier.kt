package dev.shushant.kourier.ios

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.storage.SQLiteKourierStorage
import dev.shushant.kourier.storage.db.DatabaseDriverFactory
import dev.shushant.kourier.ui.KourierUI
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.experimental.ExperimentalObjCName

/**
 * Single entry point for the Kourier SDK on iOS.
 *
 * Usage in KourierApp.init() (SwiftUI) or AppDelegate (UIKit):
 *
 *   Kourier.shared.doInit { builder in
 *       builder.redactHeaders(headers: ["Authorization"])
 *       builder.enableShakeGesture(enable: true)
 *   }
 *
 * Note: the Kotlin function is named `doInit` (via @ObjCName) because
 * `init` is a reserved keyword in Swift and cannot be called on an instance.
 */
@OptIn(ExperimentalObjCName::class)
object Kourier {

    /**
     * Initialises Kourier with an explicit config object.
     * Call once at app startup.
     */
    @ObjCName("doInit")
    fun initialize(config: KourierConfig = KourierConfig.Builder().build()) {
        val driver = DatabaseDriverFactory().createDriver()
        val storage = SQLiteKourierStorage(driver)
        KourierCore.initialize(config = config, storage = storage)
        KourierUI.startTriggers()
    }

    /**
     * Builder-DSL variant — preferred for multi-option configuration.
     *
     * Swift usage:
     *   Kourier.shared.doInit { builder in
     *       builder.maxPayloadSize(bytes: 512 * 1024)
     *   }
     */
    @ObjCName("doInit")
    fun initialize(block: KourierConfig.Builder.() -> Unit) {
        initialize(KourierConfig.Builder().apply(block).build())
    }

    /** Programmatically opens the Kourier debugger UI. */
    fun showUI() = KourierUI.show()

    /** Programmatically closes the Kourier debugger UI. */
    fun hideUI() = KourierUI.hide()

    /** Programmatically shows the floating debug bubble. */
    fun showBubble() = dev.shushant.kourier.ui.triggers.IosOverlayController.install()

    /** Programmatically hides the floating debug bubble. */
    fun hideBubble() = dev.shushant.kourier.ui.triggers.IosOverlayController.uninstall()

    /** Returns the base64-encoded string of the official Kourier bubble icon. */
    fun bubbleIconBase64(): String = dev.shushant.kourier.ui.components.KOURIER_BUBBLE_LOGO_BASE64

    /**
     * Subscribes to live telemetry stats updates.
     * Returns a cancellation lambda.
     */
    fun observeStats(callback: (total: Long, active: Int, errors: Long) -> Unit): () -> Unit {
        val job = kotlinx.coroutines.MainScope().launch {
            KourierCore.eventBus.telemetry.collect { stats ->
                callback(stats.totalRequests, stats.activeRequests, stats.errorCount)
            }
        }
        return { job.cancel() }
    }

    /** Switches active trigger to floating bubble. */
    fun switchToBubble() {
        KourierCore.updateConfig { current ->
            current.copy(
                triggerStyle = dev.shushant.kourier.core.config.TriggerStyle.FLOATING_BUBBLE,
                enableFloatingBubble = true,
                enableNotification = false
            )
        }
    }

    /** Switches active trigger to ambient notification tray (zero on-screen clutter). */
    fun switchToNotificationTray() {
        KourierCore.updateConfig { current ->
            current.copy(
                triggerStyle = dev.shushant.kourier.core.config.TriggerStyle.NOTIFICATION_TRAY,
                enableFloatingBubble = false,
                enableNotification = true
            )
        }
    }

    /** Switches active trigger to both bubble and notification tray. */
    fun switchToBoth() {
        KourierCore.updateConfig { current ->
            current.copy(
                triggerStyle = dev.shushant.kourier.core.config.TriggerStyle.BOTH,
                enableFloatingBubble = true,
                enableNotification = true
            )
        }
    }
}
