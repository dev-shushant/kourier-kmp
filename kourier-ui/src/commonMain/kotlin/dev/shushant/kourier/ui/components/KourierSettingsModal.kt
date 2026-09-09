package dev.shushant.kourier.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.config.TriggerStyle
import dev.shushant.kourier.ui.theme.KourierTheme
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors

@Composable
fun KourierSettingsModal(
    onDismiss: () -> Unit
) {
    val currentColors = LocalKourierColors.current
    val config by KourierCore.eventBus.config.collectAsState()
    val telemetry by KourierCore.eventBus.telemetry.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        KourierTheme(darkTheme = currentColors.isDark) {
            val colors = LocalKourierColors.current
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(scrollState)
                ) {
                    // ── Header Row ──────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.methodGet.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = colors.methodGet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Kourier Settings",
                                    style = KourierTypography.titleLarge,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Live Trigger & Inspection Controls",
                                    style = KourierTypography.caption,
                                    color = colors.textMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Section 1: Trigger Style ──────────────────────────
                    Text(
                        text = "TRIGGER EXPERIENCE",
                        style = KourierTypography.badgeSmall,
                        color = colors.methodGet,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose how Kourier alerts you and triggers the inspector.",
                        style = KourierTypography.caption,
                        color = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TriggerStyleOptionCard(
                            title = "Notification Tray",
                            subtitle = "Ambient notification tray on Android & iOS with [Open]/[Clear] actions (zero screen clutter)",
                            badge = "Non-Intrusive",
                            icon = Icons.Default.NotificationsActive,
                            iconColor = colors.methodGet,
                            isSelected = config.triggerStyle == TriggerStyle.NOTIFICATION_TRAY,
                            onClick = {
                                KourierCore.updateConfig { current ->
                                    current.copy(
                                        triggerStyle = TriggerStyle.NOTIFICATION_TRAY,
                                        enableNotification = true,
                                        enableFloatingBubble = false,
                                        enableShakeGesture = true
                                    )
                                }
                            }
                        )

                        TriggerStyleOptionCard(
                            title = "Floating Bubble",
                            subtitle = "Draggable on-screen overlay badge showing live request & error counters",
                            badge = "Interactive",
                            icon = Icons.Default.BubbleChart,
                            iconColor = colors.methodPatch,
                            isSelected = config.triggerStyle == TriggerStyle.FLOATING_BUBBLE,
                            onClick = {
                                KourierCore.updateConfig { current ->
                                    current.copy(
                                        triggerStyle = TriggerStyle.FLOATING_BUBBLE,
                                        enableNotification = false,
                                        enableFloatingBubble = true,
                                        enableShakeGesture = true
                                    )
                                }
                            }
                        )

                        TriggerStyleOptionCard(
                            title = "Both (Bubble + Tray)",
                            subtitle = "Simultaneous floating bubble and notification tray for maximum visibility",
                            badge = "Full Telemetry",
                            icon = Icons.Default.ElectricBolt,
                            iconColor = colors.statusSuccess,
                            isSelected = config.triggerStyle == TriggerStyle.BOTH,
                            onClick = {
                                KourierCore.updateConfig { current ->
                                    current.copy(
                                        triggerStyle = TriggerStyle.BOTH,
                                        enableNotification = true,
                                        enableFloatingBubble = true,
                                        enableShakeGesture = true
                                    )
                                }
                            }
                        )

                        TriggerStyleOptionCard(
                            title = "Shake Gesture Only",
                            subtitle = "Zero on-screen elements or overlays. Shake device to inspect",
                            badge = "Clean Screen",
                            icon = Icons.Default.Vibration,
                            iconColor = colors.textMuted,
                            isSelected = config.triggerStyle == TriggerStyle.SHAKE_ONLY,
                            onClick = {
                                KourierCore.updateConfig { current ->
                                    current.copy(
                                        triggerStyle = TriggerStyle.SHAKE_ONLY,
                                        enableNotification = false,
                                        enableFloatingBubble = false,
                                        enableShakeGesture = true
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Section 2: Traffic Capture & Call Stacks ───────────
                    Text(
                        text = "NETWORK INTERCEPTION",
                        style = KourierTypography.badgeSmall,
                        color = colors.methodPut,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Capture Call Stacks",
                                style = KourierTypography.label,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Record call-site origin frames up to ${config.maxCallStackDepth} deep",
                                style = KourierTypography.caption,
                                color = colors.textMuted
                            )
                        }

                        Switch(
                            checked = config.captureCallStack,
                            onCheckedChange = { isChecked ->
                                KourierCore.updateConfig { current ->
                                    current.copy(captureCallStack = isChecked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.methodGet,
                                checkedTrackColor = colors.methodGet.copy(alpha = 0.5f),
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.border
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── Section 3: Retention Limit ────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Max Retained Requests",
                                style = KourierTypography.label,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Current limit: ${config.maxRetentionCount} transactions",
                                style = KourierTypography.caption,
                                color = colors.textMuted
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(250, 500, 1000).forEach { count ->
                                val isSelected = config.maxRetentionCount == count
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) colors.methodGet else colors.surface)
                                        .border(
                                            1.dp,
                                            if (isSelected) colors.methodGet else colors.border,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            KourierCore.updateConfig { current ->
                                                current.copy(maxRetentionCount = count)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$count",
                                        style = KourierTypography.caption,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Section 4: Clear Transactions ─────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.statusServerError.copy(alpha = 0.08f))
                            .border(1.dp, colors.statusServerError.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable {
                                KourierCore.clearAll()
                                onDismiss()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(colors.statusServerError.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = colors.statusServerError,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Purge All Stored Transactions",
                                    style = KourierTypography.label,
                                    color = colors.statusServerError
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Deletes recorded history from SQLite database",
                                    style = KourierTypography.caption,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Footer: Version Info ──────────────────────────────
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Kourier Multiplatform · Android & iOS Debugger",
                            style = KourierTypography.caption,
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggerStyleOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalKourierColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.surfaceElevated else colors.surface)
            .border(
                1.5.dp,
                if (isSelected) colors.methodGet else colors.border,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) iconColor.copy(alpha = 0.2f) else colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) iconColor else colors.textMuted,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = KourierTypography.label,
                        color = if (isSelected) colors.textPrimary else colors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) iconColor.copy(alpha = 0.15f) else colors.border.copy(alpha = 0.4f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badge,
                            style = KourierTypography.badgeSmall,
                            color = if (isSelected) iconColor else colors.textMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = KourierTypography.caption,
                    color = colors.textMuted
                )
            }
        }
    }
}
