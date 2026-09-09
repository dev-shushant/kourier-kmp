package dev.shushant.kourier.ui.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.core.model.TransactionStatus
import dev.shushant.kourier.ui.components.MethodBadge
import dev.shushant.kourier.ui.components.StatusBadge
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors

@Composable
fun TransactionItemView(
    transaction: HttpTransaction,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current
    val resp = transaction.response

    // Determine left accent strip color
    val accentColor: Color = when {
        transaction.error != null                       -> colors.statusServerError
        transaction.status == TransactionStatus.PENDING -> colors.statusPending
        resp != null                                    -> colors.statusColor(resp.statusCode)
        else                                            -> colors.border
    }

    val bgColor     = if (isSelected) colors.surfaceElevated else colors.cardBackground
    val borderColor = if (isSelected) colors.methodGet.copy(alpha = 0.7f) else colors.border

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        // ── Left accent strip ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        // ── Card content ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Row 1: Method badge + status badge  |  Duration · Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MethodBadge(transaction.request.method)
                Spacer(modifier = Modifier.width(6.dp))
                StatusBadge(transaction)

                Spacer(modifier = Modifier.weight(1f))

                // Duration with color if slow (>3s amber, >5s red)
                val durationColor = when {
                    transaction.durationMs >= 5000L -> colors.statusServerError
                    transaction.durationMs >= 2000L -> colors.statusClientError
                    else                            -> colors.textSecondary
                }
                Text(
                    text = formatDuration(transaction.durationMs),
                    style = KourierTypography.label,
                    color = durationColor
                )

                Text(
                    text = "  ·  ",
                    style = KourierTypography.caption,
                    color = colors.textMuted
                )

                val totalSize = transaction.totalBytesSent + transaction.totalBytesReceived
                Text(
                    text = TelemetryStats.formatBytes(totalSize),
                    style = KourierTypography.label,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Path (monospace, primary color)
            Text(
                text = transaction.request.path.ifEmpty { "/" },
                style = KourierTypography.urlText,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Row 3: Host + Protocol + Content Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (transaction.request.isSsl) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "HTTPS",
                            tint = colors.textMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = transaction.request.host,
                        style = KourierTypography.caption,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val mime = resp?.contentType ?: transaction.request.contentType
                if (!mime.isNullOrBlank()) {
                    val cleanMime = when {
                        mime.contains("json", ignoreCase = true) -> "JSON"
                        mime.contains("image", ignoreCase = true) -> "IMAGE"
                        mime.contains("html", ignoreCase = true) -> "HTML"
                        mime.contains("xml", ignoreCase = true) -> "XML"
                        mime.contains("form", ignoreCase = true) -> "FORM"
                        else -> mime.substringBefore(";").substringAfterLast("/").uppercase()
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surfaceElevated)
                            .border(0.5.dp, colors.border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = cleanMime,
                            style = KourierTypography.badgeSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String = when {
    ms >= 1000L -> {
        val tenths = (ms + 50L) / 100L
        val whole = tenths / 10L
        val frac = tenths % 10L
        "${whole}.${frac}s"
    }
    else -> "${ms}ms"
}

