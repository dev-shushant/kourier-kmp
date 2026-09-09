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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.engine.CurlGenerator
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.ui.components.SyntaxHighlightedCodeView
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors
import dev.shushant.kourier.ui.utils.rememberKourierClipboard

@Composable
fun PayloadTab(
    transaction: HttpTransaction,
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current
    val clipboard = rememberKourierClipboard()
    var isRawMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Action buttons ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                icon = { Icon(Icons.Default.Code, contentDescription = null, tint = colors.methodGet, modifier = Modifier.size(15.dp)) },
                label = "Copy cURL",
                labelColor = colors.methodGet,
                onClick = {
                    val curl = CurlGenerator.generate(transaction.request)
                    clipboard.copyText(curl)
                }
            )

            if (!transaction.request.body.isNullOrBlank()) {
                ActionButton(
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp)) },
                    label = "Copy Raw",
                    labelColor = colors.textSecondary,
                    onClick = { clipboard.copyText(transaction.request.body ?: "") }
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRawMode) colors.methodGet.copy(alpha = 0.15f) else colors.surfaceElevated)
                        .border(1.dp, if (isRawMode) colors.methodGet.copy(alpha = 0.6f) else colors.border, RoundedCornerShape(8.dp))
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Body meta ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Request Body",
                style = KourierTypography.subtitle,
                color = colors.textSecondary
            )
            Text(
                text = "${TelemetryStats.formatBytes(transaction.request.contentLength)}${if (transaction.request.isTruncated) " (truncated)" else ""}",
                style = KourierTypography.caption,
                color = if (transaction.request.isTruncated) colors.statusClientError else colors.textMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (transaction.request.body.isNullOrBlank()) {
            EmptyBodyBox("No request payload (${transaction.request.method})")
        } else {
            SyntaxHighlightedCodeView(
                rawCode = transaction.request.body,
                contentType = if (isRawMode) "text/plain" else transaction.request.contentType
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: @Composable () -> Unit,
    label: String,
    labelColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val colors = LocalKourierColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = KourierTypography.label, color = labelColor)
        }
    }
}

@Composable
private fun EmptyBodyBox(message: String) {
    val colors = LocalKourierColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = KourierTypography.caption, color = colors.textMuted)
    }
}
