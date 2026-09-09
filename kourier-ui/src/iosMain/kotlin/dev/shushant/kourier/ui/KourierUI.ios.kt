package dev.shushant.kourier.ui

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.ui.controller.KourierViewController
import dev.shushant.kourier.ui.triggers.IosOverlayController
import dev.shushant.kourier.ui.triggers.IosOverlayMode
import dev.shushant.kourier.ui.triggers.NotificationTrigger
import dev.shushant.kourier.ui.triggers.ShakeDetector
import platform.UIKit.UIApplication
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

actual object KourierUI {
    private var shakeDetector: ShakeDetector? = null

    private fun getActiveWindow(): UIWindow? {
        return try {
            val app = UIApplication.sharedApplication
            @Suppress("SENSELESS_COMPARISON")
            val scenes = app.connectedScenes
            @Suppress("SENSELESS_COMPARISON")
            if (scenes == null) return null
            val activeScene = scenes.filterIsInstance<UIWindowScene>().firstOrNull {
                it.activationState == UISceneActivationStateForegroundActive
            } ?: scenes.filterIsInstance<UIWindowScene>().firstOrNull()

            val overlayWindow = IosOverlayController.overlayWindow
            val windows = (activeScene?.windows?.filterIsInstance<UIWindow>() ?: emptyList())
                .filter { it !== overlayWindow }  // never use the Kourier bubble window
            windows.firstOrNull { it.isKeyWindow() }
                ?: windows.firstOrNull()
                ?: app.keyWindow
        } catch (_: Throwable) {
            null
        }
    }

    private var isShowing = false

    actual fun show() {
        if (!platform.Foundation.NSThread.isMainThread()) {
            platform.Foundation.NSOperationQueue.mainQueue.addOperationWithBlock {
                show()
            }
            return
        }
        if (isShowing) return
        try {
            val window = getActiveWindow() ?: return
            var topController = window.rootViewController ?: return
            while (topController.presentedViewController != null) {
                topController = topController.presentedViewController!!
            }

            if (topController.isBeingPresented() || topController.isBeingDismissed()) {
                return
            }

            isShowing = true
            IosOverlayController.setVisible(false)
            val kourierVC = KourierViewController.create(onDismiss = {
                isShowing = false
                IosOverlayController.setVisible(true)
            })
            kourierVC.modalPresentationStyle = UIModalPresentationFullScreen
            topController.presentViewController(kourierVC, animated = true, completion = null)
        } catch (_: Throwable) {
            isShowing = false
            IosOverlayController.setVisible(true)
        }
    }

    actual fun hide() {
        if (!platform.Foundation.NSThread.isMainThread()) {
            platform.Foundation.NSOperationQueue.mainQueue.addOperationWithBlock {
                hide()
            }
            return
        }
        isShowing = false
        IosOverlayController.setVisible(true)
        try {
            val window = getActiveWindow() ?: return
            window.rootViewController?.dismissViewControllerAnimated(true, completion = null)
        } catch (_: Throwable) {}
    }

    private var triggersScope: CoroutineScope? = null
    private var notificationJob: Job? = null

    @OptIn(FlowPreview::class)
    actual fun startTriggers() {
        if (triggersScope != null) return
        val scope = CoroutineScope(Dispatchers.Main + Job())
        triggersScope = scope

        scope.launch {
            KourierCore.eventBus.events.collect { event ->
                when (event) {
                    is dev.shushant.kourier.core.engine.KourierEvent.TransactionCompleted -> {
                        NotificationTrigger.recordTransaction(event.transaction)
                    }
                    is dev.shushant.kourier.core.engine.KourierEvent.TransactionsCleared -> {
                        NotificationTrigger.clearTransactions()
                    }
                    else -> {}
                }
            }
        }

        scope.launch {
            KourierCore.eventBus.config.collect { config ->
                try {
                    // Shake detector
                    if (config.enableShakeGesture) {
                        if (shakeDetector == null) {
                            shakeDetector = ShakeDetector().apply {
                                start { show() }
                            }
                        }
                    } else {
                        shakeDetector?.stop()
                        shakeDetector = null
                    }

                    // Notification trigger (live system notifications)
                    if (config.enableNotification) {
                        if (notificationJob == null || notificationJob?.isActive == false) {
                            notificationJob = launch(Dispatchers.Default) {
                                KourierCore.eventBus.telemetry
                                    .debounce(500L.milliseconds)
                                    .collect { telemetry ->
                                        NotificationTrigger.updateNotification(telemetry)
                                    }
                            }
                        }
                    } else {
                        notificationJob?.cancel()
                        notificationJob = null
                        NotificationTrigger.dismiss()
                    }

                    // Overlay is strictly for the floating bubble — notification tray mode leaves the screen completely clear
                    if (config.enableFloatingBubble) {
                        IosOverlayController.install(IosOverlayMode.BUBBLE)
                    } else {
                        IosOverlayController.uninstall()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    actual fun stopTriggers() {
        try {
            notificationJob?.cancel()
            notificationJob = null
            NotificationTrigger.dismiss()
            triggersScope?.cancel()
            triggersScope = null
            shakeDetector?.stop()
            shakeDetector = null
            IosOverlayController.uninstall()
        } catch (_: Throwable) {}
    }
}
