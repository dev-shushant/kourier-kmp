package dev.shushant.kourier.ui.triggers

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.ui.KourierUI
import dev.shushant.kourier.ui.theme.KourierTheme
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
import platform.UIKit.UIView
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelStatusBar
import platform.UIKit.UIWindowScene
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UISceneDidActivateNotification

// UIWindowLevelStatusBar (1000) keeps the overlay above all app content but
// below system alerts (UIWindowLevelAlert = 2000).
private val OVERLAY_WINDOW_LEVEL = UIWindowLevelStatusBar - 1.0

enum class IosOverlayMode {
    BUBBLE
}

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
private class PassthroughUIWindow : UIWindow {
    @OverrideInit
    constructor(windowScene: UIWindowScene) : super(windowScene = windowScene)

    var overlayX: Double = -1000.0
    var overlayY: Double = 140.0
    var overlayWidth: Double = 58.0
    var overlayHeight: Double = 58.0

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        val hitView = super.hitTest(point, withEvent) ?: return null
        var isInside = false
        point.useContents {
            val margin = 16.0
            isInside = x >= (overlayX - margin) && x <= (overlayX + overlayWidth + margin) &&
                       y >= (overlayY - margin) && y <= (overlayY + overlayHeight + margin)
        }
        return if (isInside) hitView else null
    }
}

@OptIn(ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
object IosOverlayController {
    internal var overlayWindow: UIWindow? = null
    private var currentMode: IosOverlayMode = IosOverlayMode.BUBBLE
    private var sceneObserver: Any? = null

    fun install(mode: IosOverlayMode = IosOverlayMode.BUBBLE) {
        // UIKit must be mutated on the main thread.
        if (!NSThread.isMainThread()) {
            NSOperationQueue.mainQueue.addOperationWithBlock { install(mode) }
            return
        }
        try {
            if (overlayWindow != null) {
                if (currentMode == mode) return
                uninstall()
            }
            currentMode = mode

            val scene = activeScene()
            if (scene != null) {
                installOnScene(scene, mode)
            } else {
                sceneObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = UISceneDidActivateNotification,
                    `object` = null,
                    queue = NSOperationQueue.mainQueue
                ) { _ ->
                    sceneObserver?.let {
                        NSNotificationCenter.defaultCenter.removeObserver(it)
                        sceneObserver = null
                    }
                    if (overlayWindow == null) {
                        activeScene()?.let { s -> installOnScene(s, mode) }
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun activeScene(): UIWindowScene? {
        val app = UIApplication.sharedApplication
        @Suppress("SENSELESS_COMPARISON")
        val scenes = app.connectedScenes
        @Suppress("SENSELESS_COMPARISON")
        if (scenes == null) return null
        return scenes.filterIsInstance<UIWindowScene>().firstOrNull {
            it.activationState == UISceneActivationStateForegroundActive
        } ?: scenes.filterIsInstance<UIWindowScene>().firstOrNull()
    }

    private fun installOnScene(scene: UIWindowScene, mode: IosOverlayMode) {
        try {
            val window = PassthroughUIWindow(windowScene = scene)
            window.backgroundColor = UIColor.clearColor
            window.windowLevel = OVERLAY_WINDOW_LEVEL
            window.setUserInteractionEnabled(true)

            // Seed initial position in UIKit points for floating debug bubble
            scene.coordinateSpace.bounds.useContents {
                window.overlayWidth = 58.0
                window.overlayHeight = 58.0
                window.overlayX = size.width - 58.0 - 16.0
                window.overlayY = 140.0
            }

            val overlayVC = ComposeUIViewController(configure = {
                opaque = false
            }) {
                KourierTheme {
                    val telemetry by KourierCore.eventBus.telemetry.collectAsState()
                    FloatingDebugBubble(
                        telemetry = telemetry,
                        onClick = { KourierUI.show() },
                        onPositionChanged = { x, y, size ->
                            window.overlayX = x.toDouble()
                            window.overlayY = y.toDouble()
                            window.overlayWidth = size.toDouble()
                            window.overlayHeight = size.toDouble()
                        }
                    )
                }
            }
            overlayVC.view.backgroundColor = UIColor.clearColor

            window.rootViewController = overlayVC
            window.hidden = false
            overlayWindow = window
        } catch (_: Throwable) {}
    }

    fun uninstall() {
        // UIKit must be mutated on the main thread.
        if (!NSThread.isMainThread()) {
            NSOperationQueue.mainQueue.addOperationWithBlock { uninstall() }
            return
        }
        try {
            sceneObserver?.let {
                NSNotificationCenter.defaultCenter.removeObserver(it)
                sceneObserver = null
            }
            overlayWindow?.hidden = true
            overlayWindow?.rootViewController = null
            overlayWindow = null
        } catch (_: Throwable) {}
    }

    fun setVisible(visible: Boolean) {
        if (!NSThread.isMainThread()) {
            NSOperationQueue.mainQueue.addOperationWithBlock { setVisible(visible) }
            return
        }
        overlayWindow?.hidden = !visible
    }
}
