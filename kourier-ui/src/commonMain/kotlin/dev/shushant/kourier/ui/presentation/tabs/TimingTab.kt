package dev.shushant.kourier.ui.presentation.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors

@Composable
fun TimingTab(
    transaction: HttpTransaction,
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current

    val totalMs = transaction.durationMs.coerceAtLeast(1L)
    val ttfbMs = transaction.timings.timeToFirstByteMs.coerceAtLeast(0L)
    val responseMs = transaction.timings.responseDurationMs.coerceAtLeast(0L)
    val requestMs = (totalMs - ttfbMs - responseMs).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Timing waterfall  ·  ${totalMs} ms total",
            style = KourierTypography.subtitle,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stacked waterfall bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, RoundedCornerShape(4.dp))
        ) {
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                val reqWeight  = (requestMs.toFloat()  / totalMs.toFloat()).coerceIn(0.02f, 1f)
                val ttfbWeight = (ttfbMs.toFloat()     / totalMs.toFloat()).coerceIn(0.02f, 1f)
                val respWeight = (responseMs.toFloat() / totalMs.toFloat()).coerceIn(0.02f, 1f)

                Box(modifier = Modifier.weight(reqWeight).fillMaxHeight().background(colors.methodGet))
                Box(modifier = Modifier.weight(ttfbWeight).fillMaxHeight().background(colors.statusClientError))
                Box(modifier = Modifier.weight(respWeight).fillMaxHeight().background(colors.statusSuccess))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TimingPhaseRow(
            color = colors.methodGet,
            name = "Request write / dispatch",
            durationMs = requestMs,
            percent = (requestMs * 100 / totalMs).toInt()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TimingPhaseRow(
            color = colors.statusClientError,
            name = "Waiting (TTFB)",
            durationMs = ttfbMs,
            percent = (ttfbMs * 100 / totalMs).toInt()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TimingPhaseRow(
            color = colors.statusSuccess,
            name = "Content download / read",
            durationMs = responseMs,
            percent = (responseMs * 100 / totalMs).toInt()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = colors.divider)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total elapsed",
                style = KourierTypography.subtitle,
                color = colors.textPrimary
            )
            Text(
                text = "$totalMs ms",
                style = KourierTypography.subtitle,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun TimingPhaseRow(
    color: Color,
    name: String,
    durationMs: Long,
    percent: Int
) {
    val colors = LocalKourierColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            style = KourierTypography.body,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$durationMs ms ($percent%)",
            style = KourierTypography.codeSmall,
            color = colors.textSecondary
        )
    }
}
