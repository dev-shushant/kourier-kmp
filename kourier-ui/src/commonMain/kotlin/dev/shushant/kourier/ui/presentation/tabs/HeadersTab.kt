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
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.model.HttpHeader
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors
import dev.shushant.kourier.ui.utils.rememberKourierClipboard

@Composable
fun HeadersTab(
    transaction: HttpTransaction,
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current
    val clipboard = rememberKourierClipboard()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Request Headers ──────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Request Headers (${transaction.request.headers.size})",
                style = KourierTypography.subtitle,
                color = colors.methodGet
            )
            IconButton(
                onClick = {
                    val allReq = transaction.request.headers.joinToString("\n") { "${it.name}: ${it.value}" }
                    clipboard.copyText(allReq)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy all request headers",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HeaderTable(headers = transaction.request.headers)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Response Headers ─────────────────────────────────────────────────
        val respHeaders = transaction.response?.headers ?: emptyList()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Response Headers (${respHeaders.size})",
                style = KourierTypography.subtitle,
                color = colors.statusSuccess
            )
            if (respHeaders.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val allResp = respHeaders.joinToString("\n") { "${it.name}: ${it.value}" }
                        clipboard.copyText(allResp)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy all response headers",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (respHeaders.isEmpty()) {
            Text(
                text = if (transaction.isPending) "Response pending…" else "No response headers available",
                style = KourierTypography.caption,
                color = colors.textMuted
            )
        } else {
            HeaderTable(headers = respHeaders)
        }
    }
}

@Composable
private fun HeaderTable(headers: List<HttpHeader>) {
    val colors = LocalKourierColors.current
    val clipboard = rememberKourierClipboard()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
    ) {
        headers.forEachIndexed { index, header ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { clipboard.copyText("${header.name}: ${header.value}") }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = header.name,
                    style = KourierTypography.codeSmall.copy(color = colors.syntaxKey),
                    modifier = Modifier.weight(0.4f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(modifier = Modifier.weight(0.6f), verticalAlignment = Alignment.CenterVertically) {
                    if (header.isRedacted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.statusClientError.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "REDACTED",
                                style = KourierTypography.badge,
                                color = colors.statusClientError
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = header.value,
                        style = KourierTypography.codeSmall,
                        color = colors.textPrimary
                    )
                }
            }
            if (index < headers.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
            }
        }
    }
}
