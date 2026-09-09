package dev.shushant.kourier.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors

@Composable
fun FilterChipRow(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // ── Search bar ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(
                    1.dp,
                    if (searchQuery.isNotEmpty()) colors.methodGet.copy(alpha = 0.6f) else colors.border,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (searchQuery.isNotEmpty()) colors.methodGet else colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search URL, path, host, body…",
                        style = KourierTypography.bodySmall,
                        color = colors.textMuted
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = KourierTypography.bodySmall.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.methodGet),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AnimatedVisibility(
                visible = searchQuery.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.border)
                        .clickable { onSearchQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Status + Method chips ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusFilters = listOf(
                "ALL" to colors.methodGet,
                "ERRORS" to colors.statusServerError,
                "2XX" to colors.statusSuccess,
                "3XX" to colors.statusRedirect,
                "4XX" to colors.statusClientError,
                "5XX" to colors.statusServerError
            )
            statusFilters.forEach { (status, accent) ->
                FilterChip(
                    text = status,
                    isSelected = selectedStatus.equals(status, ignoreCase = true),
                    activeColor = accent,
                    onClick = { onStatusSelected(status) }
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Vertical separator
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(colors.border)
            )
            Spacer(modifier = Modifier.width(10.dp))

            val methodFilters = listOf("ALL", "GET", "POST", "PUT", "DELETE", "PATCH")
            methodFilters.forEach { method ->
                val methodAccent = if (method == "ALL") colors.methodGet else colors.methodColor(method)
                FilterChip(
                    text = method,
                    isSelected = selectedMethod.equals(method, ignoreCase = true),
                    activeColor = methodAccent,
                    onClick = { onMethodSelected(method) }
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalKourierColors.current
    val bgColor = if (isSelected) {
        if (colors.isDark) activeColor.copy(alpha = 0.16f) else activeColor.copy(alpha = 0.12f)
    } else {
        colors.surfaceElevated
    }
    val borderColor = if (isSelected) activeColor.copy(alpha = 0.6f) else colors.border
    val textColor   = if (isSelected) activeColor else colors.textSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = KourierTypography.label,
            color = textColor
        )
    }
}

