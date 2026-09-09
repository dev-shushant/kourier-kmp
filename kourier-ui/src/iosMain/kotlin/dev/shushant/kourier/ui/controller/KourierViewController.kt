package dev.shushant.kourier.ui.controller

import androidx.compose.ui.window.ComposeUIViewController
import dev.shushant.kourier.ui.presentation.KourierRootScreen
import platform.UIKit.UIViewController

object KourierViewController {
    /**
     * Creates a UIViewController displaying the Kourier debugger UI.
     * Can be embedded or presented directly from Swift/iOS.
     */
    fun create(onDismiss: (() -> Unit)? = null): UIViewController {
        lateinit var controller: UIViewController
        controller = ComposeUIViewController {
            KourierRootScreen(
                onClose = {
                    controller.dismissViewControllerAnimated(true) {
                        onDismiss?.invoke()
                    }
                }
            )
        }
        return controller
    }
}
