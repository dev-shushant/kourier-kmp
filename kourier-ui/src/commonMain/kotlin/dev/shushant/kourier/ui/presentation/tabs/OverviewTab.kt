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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.engine.CurlGenerator
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.ui.components.MethodBadge
import dev.shushant.kourier.ui.components.StatusBadge
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors
import dev.shushant.kourier.ui.utils.rememberKourierClipboard

@Composable
fun OverviewTab(
    transaction: HttpTransaction,
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current
    val clipboard = rememberKourierClipboard()
    val resp = transaction.response

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Primary Summary Card ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MethodBadge(transaction.request.method)
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(transaction)
                        if (transaction.request.isSsl) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.statusSuccess.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "TLS/HTTPS",
                                        tint = colors.statusSuccess,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "HTTPS",
                                        style = KourierTypography.badgeSmall,
                                        color = colors.statusSuccess
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick cURL copy
                        IconButton(
                            onClick = {
                                val curl = CurlGenerator.generate(transaction.request)
                                clipboard.copyText(curl)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Copy cURL",
                                tint = colors.methodGet,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Quick URL copy
                        IconButton(
                            onClick = { clipboard.copyText(transaction.request.url) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy URL",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Full URL
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.codeBackground)
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                        .clickable { clipboard.copyText(transaction.request.url) }
                        .padding(10.dp)
                ) {
                    Text(
                        text = transaction.request.url,
                        style = KourierTypography.urlText,
                        color = colors.textPrimary
                    )
                }
            }
        }

        // ── Failure Reason Alert Card (if error) ────────────────────────────
        val txError = transaction.error
        if (txError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.statusServerError.copy(alpha = 0.08f))
                    .border(1.dp, colors.statusServerError.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = colors.statusServerError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = txError.exceptionClass,
                            style = KourierTypography.titleLarge,
                            color = colors.statusServerError
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = txError.message,
                        style = KourierTypography.body,
                        color = colors.textPrimary
                    )
                }
            }
        }

        // ── Network Metrics Grid ────────────────────────────────────────────
        Column {
            Text(
                text = "Network Metrics",
                style = KourierTypography.subtitle,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            ) {
                Column {
                    InfoRow("Duration", formatDuration(transaction.durationMs), onClick = {
                        clipboard.copyText("${transaction.durationMs} ms")
                    })
                    RowDivider()
                    InfoRow("Time to First Byte", "${transaction.timings.timeToFirstByteMs} ms")
                    RowDivider()
                    InfoRow("Protocol", transaction.protocol ?: "HTTP/1.1")
                    RowDivider()
                    InfoRow("Remote Address", transaction.remoteAddress ?: "—", onClick = {
                        transaction.remoteAddress?.let { clipboard.copyText(it) }
                    })
                    RowDivider()
                    InfoRow("Request Size", TelemetryStats.formatBytes(transaction.totalBytesSent))
                    RowDivider()
                    InfoRow("Response Size", TelemetryStats.formatBytes(transaction.totalBytesReceived))
                    RowDivider()
                    InfoRow("Req Content-Type", transaction.request.contentType ?: "—")
                    RowDivider()
                    InfoRow("Resp Content-Type", resp?.contentType ?: "—")
                }
            }
        }
    }
}

@Composable
private fun RowDivider() {
    val colors = LocalKourierColors.current
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalKourierColors.current
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = KourierTypography.bodySmall, color = colors.textMuted)
        Text(text = value, style = KourierTypography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = colors.textPrimary)
    }
}

private fun formatDuration(ms: Long): String = when {
    ms >= 1000L -> {
        val tenths = (ms + 50L) / 100L
        val whole = tenths / 10L
        val frac = tenths % 10L
        "${whole}.${frac}s"
    }
    else        -> "${ms} ms"
}

