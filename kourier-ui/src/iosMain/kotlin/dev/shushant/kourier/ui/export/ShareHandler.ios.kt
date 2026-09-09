package dev.shushant.kourier.ui.export

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

@OptIn(ExperimentalForeignApi::class)
actual object ShareHandler {
    actual fun shareText(title: String, content: String, chooserTitle: String) {
        val window = UIApplication.sharedApplication.keyWindow ?: return
        var rootViewController = window.rootViewController
        while (rootViewController?.presentedViewController != null) {
            rootViewController = rootViewController.presentedViewController
        }

        val activityViewController = UIActivityViewController(
            activityItems = listOf(content),
            applicationActivities = null
        )

        rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null
        )
    }

    actual fun shareFile(fileName: String, content: String, mimeType: String, chooserTitle: String) {
        val tempDir = NSTemporaryDirectory()
        val filePath = "$tempDir$fileName"
        val file = fopen(filePath, "w")
        if (file != null) {
            fputs(content, file)
            fclose(file)
        }

        val fileUrl = NSURL.fileURLWithPath(filePath)
        val window = UIApplication.sharedApplication.keyWindow ?: return
        var rootViewController = window.rootViewController
        while (rootViewController?.presentedViewController != null) {
            rootViewController = rootViewController.presentedViewController
        }

        val activityViewController = UIActivityViewController(
            activityItems = listOf(fileUrl),
            applicationActivities = null
        )

        rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null
        )
    }
}
