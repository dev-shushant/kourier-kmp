package dev.shushant.kourier.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image as SkiaImage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
actual fun KourierSquareLogo(modifier: Modifier) {
    val bitmap = remember { decodeSquareLogo() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Kourier",
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00D2FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
actual fun KourierBubbleImage(modifier: Modifier) {
    val bitmap = remember { decodeBubbleLogo() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Kourier Debug Bubble",
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0xFF00D2FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun decodeSquareLogo(): ImageBitmap? {
    return try {
        val bytes = Base64.decode(KOURIER_SQUARE_LOGO_BASE64)
        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Throwable) {
        null
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun decodeBubbleLogo(): ImageBitmap? {
    return try {
        val bytes = Base64.decode(KOURIER_BUBBLE_LOGO_BASE64)
        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Throwable) {
        null
    }
}
