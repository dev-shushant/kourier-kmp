package dev.shushant.kourier.ui.presentation.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.ui.components.SyntaxHighlightedCodeView
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors
import dev.shushant.kourier.ui.utils.rememberKourierClipboard

@Composable
fun ResponseTab(
    transaction: HttpTransaction,
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current
    val clipboard = rememberKourierClipboard()
    val resp = transaction.response
    var isRawMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val responseBody = resp?.body
        val contentType = resp?.contentType ?: ""
        val isImage = contentType.startsWith("image/", ignoreCase = true)

        // Action row
        if (responseBody != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                        .clickable {
                            clipboard.copyText(responseBody)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy response body",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy Body",
                            style = KourierTypography.label,
                            color = colors.textSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isRawMode) colors.methodGet.copy(alpha = 0.15f) else colors.surfaceElevated)
                        .border(1.dp, if (isRawMode) colors.methodGet.copy(alpha = 0.6f) else colors.border, RoundedCornerShape(6.dp))
                        .clickable { isRawMode = !isRawMode }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isRawMode) "Mode: Raw" else "Mode: Formatted",
                        style = KourierTypography.label,
                        color = if (isRawMode) colors.methodGet else colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isImage) "Image Response" else "Response Body",
                style = KourierTypography.subtitle,
                color = colors.textSecondary
            )
            if (resp != null) {
                Text(
                    text = TelemetryStats.formatBytes(resp.contentLength) +
                        if (resp.isTruncated) " (truncated)" else "",
                    style = KourierTypography.caption,
                    color = if (resp.isTruncated) colors.statusClientError else colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            transaction.isPending -> {
                EmptyBodyBox(
                    text = "Awaiting response from server…",
                    textColor = colors.statusPending,
                    borderColor = colors.border,
                    surfaceColor = colors.surfaceElevated
                )
            }
            isImage -> {
                ImageResponsePreview(
                    contentType = contentType,
                    contentLength = resp?.contentLength ?: 0L,
                    body = responseBody
                )
            }
            responseBody.isNullOrBlank() -> {
                EmptyBodyBox(
                    text = "Empty response body",
                    textColor = colors.textMuted,
                    borderColor = colors.border,
                    surfaceColor = colors.surfaceElevated
                )
            }
            else -> {
                SyntaxHighlightedCodeView(
                    rawCode = responseBody,
                    contentType = if (isRawMode) "text/plain" else (resp.contentType ?: "text/plain")
                )
            }
        }
    }
}

@Composable
private fun ImageResponsePreview(
    contentType: String,
    contentLength: Long,
    body: String?
) {
    val colors = LocalKourierColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Image preview",
                tint = colors.statusSuccess,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = contentType,
                style = KourierTypography.title,
                color = colors.textPrimary
            )
            Text(
                text = "Size: ${TelemetryStats.formatBytes(contentLength)}",
                style = KourierTypography.caption,
                color = colors.textMuted
            )
            if (!body.isNullOrBlank()) {
                Text(
                    text = "Payload preview / binary stream captured (${body.length} chars)",
                    style = KourierTypography.caption,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyBodyBox(
    text: String,
    textColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = KourierTypography.body,
            color = textColor
        )
    }
}
