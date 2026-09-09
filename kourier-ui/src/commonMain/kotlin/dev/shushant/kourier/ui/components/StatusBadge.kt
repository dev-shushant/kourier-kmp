package dev.shushant.kourier.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TransactionStatus
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors

@Composable
fun StatusBadge(transaction: HttpTransaction, modifier: Modifier = Modifier) {
    val colors = LocalKourierColors.current
    val resp = transaction.response
    val isPending = transaction.status == TransactionStatus.PENDING

    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val (bgColor, borderColor, textColor, text) = when {
        isPending -> {
            val c = colors.statusPending
            val bg = if (colors.isDark) c.copy(alpha = 0.16f) else c.copy(alpha = 0.12f)
            val b = if (colors.isDark) c.copy(alpha = 0.35f) else c.copy(alpha = 0.3f)
            Tuple4(bg, b, c, "PENDING")
        }
        transaction.error != null -> {
            val c = colors.statusServerError
            val bg = if (colors.isDark) c.copy(alpha = 0.16f) else c.copy(alpha = 0.12f)
            val b = if (colors.isDark) c.copy(alpha = 0.35f) else c.copy(alpha = 0.3f)
            Tuple4(bg, b, c, "FAIL")
        }
        resp != null -> {
            val code = resp.statusCode
            val c = colors.statusColor(code)
            val bg = if (colors.isDark) c.copy(alpha = 0.16f) else c.copy(alpha = 0.12f)
            val b = if (colors.isDark) c.copy(alpha = 0.35f) else c.copy(alpha = 0.3f)
            Tuple4(bg, b, c, code.toString())
        }
        else -> Tuple4(colors.surfaceElevated, colors.border, colors.textSecondary, "—")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isPending) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(textColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = textColor,
                style = KourierTypography.badge
            )
        }
    }
}

@Composable
fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val colors = LocalKourierColors.current
    val color = colors.methodColor(method)
    val bgColor = if (colors.isDark) color.copy(alpha = 0.14f) else color.copy(alpha = 0.12f)
    val borderColor = if (colors.isDark) color.copy(alpha = 0.35f) else color.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = method.uppercase(),
            color = color,
            style = KourierTypography.badge
        )
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

