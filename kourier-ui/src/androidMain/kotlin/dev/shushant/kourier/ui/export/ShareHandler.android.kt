package dev.shushant.kourier.ui.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object AndroidContextHolder {
    var applicationContext: Context? = null
    var currentActivity: Context? = null
}

actual object ShareHandler {
    actual fun shareText(title: String, content: String, chooserTitle: String) {
        val context = AndroidContextHolder.currentActivity ?: AndroidContextHolder.applicationContext ?: return
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    }

    actual fun shareFile(fileName: String, content: String, mimeType: String, chooserTitle: String) {
        val context = AndroidContextHolder.currentActivity ?: AndroidContextHolder.applicationContext ?: return
        try {
            val cacheDir = File(context.cacheDir, "kourier").apply { mkdirs() }
            val file = File(cacheDir, fileName)
            file.writeText(content)

            val authority = "${context.packageName}.kourier.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, fileName)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                clipData = android.content.ClipData.newRawUri(fileName, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                clipData = android.content.ClipData.newRawUri(fileName, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to text sharing if file writing or FileProvider fails
            shareText(title = fileName, content = content, chooserTitle = chooserTitle)
        }
    }
}
