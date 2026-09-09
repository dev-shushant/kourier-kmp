package dev.shushant.kourier.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors

@Composable
fun StickyTelemetryBottomBar(
    telemetry: TelemetryStats,
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current

    // Pulsing animation when interceptor is active
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (telemetry.interceptorActive) 1.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceElevated)
    ) {
        // Top divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(46.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Live / Recording status pill ──────────────────────────────────
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (telemetry.interceptorActive) colors.statusSuccess.copy(alpha = 0.12f)
                        else colors.statusServerError.copy(alpha = 0.12f)
                    )
                    .border(
                        1.dp,
                        if (telemetry.interceptorActive) colors.statusSuccess.copy(alpha = 0.35f)
                        else colors.statusServerError.copy(alpha = 0.35f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                if (telemetry.interceptorActive) colors.statusSuccess
                                else colors.statusServerError
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (telemetry.interceptorActive) "LIVE" else "INACTIVE",
                        style = KourierTypography.badgeSmall,
                        color = if (telemetry.interceptorActive) colors.statusSuccess else colors.statusServerError
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Counters ──────────────────────────────────────────────────────
            TelemetryChip(
                label = "REQ",
                value = "${telemetry.totalRequests}",
                valueColor = colors.textPrimary,
                badge = if (telemetry.activeRequests > 0) "${telemetry.activeRequests}↑" else null,
                badgeColor = colors.statusPending
            )

            Spacer(modifier = Modifier.width(8.dp))

            TelemetryChip(
                label = "ERR",
                value = "${telemetry.errorCount}",
                valueColor = if (telemetry.errorCount > 0) colors.statusServerError else colors.textSecondary
            )

            Spacer(modifier = Modifier.width(8.dp))

            // ── Bandwidth (Upload / Download) ─────────────────────────────────
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "↑ ${TelemetryStats.formatBytes(telemetry.totalBytesSent)}",
                        style = KourierTypography.caption,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "↓ ${TelemetryStats.formatBytes(telemetry.totalBytesReceived)}",
                        style = KourierTypography.caption,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryChip(
    label: String,
    value: String,
    valueColor: Color,
    badge: String? = null,
    badgeColor: Color? = null
) {
    val colors = LocalKourierColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label ",
                style = KourierTypography.caption,
                color = colors.textMuted
            )
            Text(
                text = value,
                style = KourierTypography.label,
                color = valueColor
            )
            if (badge != null && badgeColor != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($badge)",
                    style = KourierTypography.caption,
                    color = badgeColor
                )
            }
        }
    }
}

