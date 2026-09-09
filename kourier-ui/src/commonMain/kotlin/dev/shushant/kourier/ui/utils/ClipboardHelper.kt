package dev.shushant.kourier.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/**
 * Encapsulates clipboard copying across Compose Multiplatform, suppressing deprecation
 * warnings until Compose Multiplatform stabilizes a cross-platform ClipEntry API.
 */
@Suppress("DEPRECATION")
class KourierClipboard(
    private val clipboardManager: ClipboardManager
) {
    fun copyText(text: String) {
        clipboardManager.setText(AnnotatedString(text))
    }
}

@Composable
fun rememberKourierClipboard(): KourierClipboard {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    return KourierClipboard(clipboardManager)
}
