package dev.shushant.kourier.ui.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.engine.CurlGenerator
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.ui.components.MethodBadge
import dev.shushant.kourier.ui.components.SingleTransactionShareDialog
import dev.shushant.kourier.ui.components.StatusBadge
import dev.shushant.kourier.ui.presentation.tabs.CallStackTab
import dev.shushant.kourier.ui.presentation.tabs.HeadersTab
import dev.shushant.kourier.ui.presentation.tabs.OverviewTab
import dev.shushant.kourier.ui.presentation.tabs.PayloadTab
import dev.shushant.kourier.ui.presentation.tabs.ResponseTab
import dev.shushant.kourier.ui.presentation.tabs.TimingTab
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors
import dev.shushant.kourier.ui.utils.rememberKourierClipboard

enum class DetailTab(val label: String) {
    OVERVIEW("Overview"),
    HEADERS("Headers"),
    PAYLOAD("Payload"),
    RESPONSE("Response"),
    TIMING("Timing"),
    CALL_STACK("Call Stack")
}

@Composable
fun TransactionDetailScreen(
    transaction: HttpTransaction,
    onBack: (() -> Unit)?,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DetailTab.OVERVIEW) }
    var showShareDialog by remember { mutableStateOf(false) }
    val colors = LocalKourierColors.current
    val clipboard = rememberKourierClipboard()

    if (showShareDialog) {
        SingleTransactionShareDialog(
            transaction = transaction,
            onDismiss = { showShareDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // ── Top Header Container (Extends into Status Bar) ─────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .statusBarsPadding()
        ) {
            // App Bar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                MethodBadge(transaction.request.method)
                Spacer(modifier = Modifier.width(6.dp))
                StatusBadge(transaction)
                Spacer(modifier = Modifier.width(10.dp))

                // Path / Host
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.request.path.ifEmpty { "/" },
                        style = KourierTypography.title,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = transaction.request.host,
                        style = KourierTypography.caption,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action: Copy cURL
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            val curl = CurlGenerator.generate(transaction.request)
                            clipboard.copyText(curl)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Copy cURL",
                            tint = colors.methodGet,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Action: Theme Toggle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = if (isDarkTheme) colors.methodPut else colors.methodPatch,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Action: Share single transaction
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share transaction",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Divider below App Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )

            // ── Segmented Tab Bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val tabBadge = when (tab) {
                        DetailTab.OVERVIEW   -> null
                        DetailTab.HEADERS    -> "${transaction.request.headers.size + (transaction.response?.headers?.size ?: 0)}"
                        DetailTab.PAYLOAD    -> if (transaction.request.contentLength > 0) TelemetryStats.formatBytes(transaction.request.contentLength) else null
                        DetailTab.RESPONSE   -> if (transaction.isPending) "…" else if ((transaction.response?.contentLength ?: 0L) > 0) TelemetryStats.formatBytes(transaction.response!!.contentLength) else null
                        DetailTab.TIMING     -> "${transaction.durationMs}ms"
                        DetailTab.CALL_STACK -> if (transaction.callStack.isNotEmpty()) "${transaction.callStack.size}" else null
                    }

                    val activeBg = if (isSelected) colors.methodGet.copy(alpha = 0.15f) else colors.surfaceElevated
                    val activeBorder = if (isSelected) colors.methodGet.copy(alpha = 0.7f) else colors.border
                    val textColor = if (isSelected) colors.methodGet else colors.textSecondary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeBg)
                            .border(1.dp, activeBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tab.label,
                                style = KourierTypography.label,
                                color = textColor
                            )
                            if (tabBadge != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) colors.methodGet.copy(alpha = 0.25f) else colors.background)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = tabBadge,
                                        style = KourierTypography.badgeSmall,
                                        color = if (isSelected) colors.methodGet else colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Divider below Tab Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )
        }

        // ── Tab content ────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                DetailTab.OVERVIEW   -> OverviewTab(transaction = transaction)
                DetailTab.HEADERS    -> HeadersTab(transaction = transaction)
                DetailTab.PAYLOAD    -> PayloadTab(transaction = transaction)
                DetailTab.RESPONSE   -> ResponseTab(transaction = transaction)
                DetailTab.TIMING     -> TimingTab(transaction = transaction)
                DetailTab.CALL_STACK -> CallStackTab(transaction = transaction)
            }
        }
    }
}

