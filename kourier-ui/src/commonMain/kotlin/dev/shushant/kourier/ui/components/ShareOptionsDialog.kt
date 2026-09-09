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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Forum
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.shushant.kourier.core.engine.CurlGenerator
import dev.shushant.kourier.core.engine.HarExporter
import dev.shushant.kourier.core.engine.TransactionFormatter
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.ui.export.ShareHandler
import dev.shushant.kourier.ui.theme.KourierTheme
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors

@Composable
fun SingleTransactionShareDialog(
    transaction: HttpTransaction,
    onDismiss: () -> Unit
) {
    val currentColors = LocalKourierColors.current

    Dialog(onDismissRequest = onDismiss) {
        KourierTheme(darkTheme = currentColors.isDark) {
            val colors = LocalKourierColors.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share Transaction",
                            style = KourierTypography.titleLarge,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${transaction.request.method} ${transaction.request.path.ifEmpty { "/" }}",
                            style = KourierTypography.caption,
                            color = colors.textMuted,
                            maxLines = 1
                        )
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
                Spacer(modifier = Modifier.height(12.dp))

                // Option 1: Plain Text (WhatsApp, Slack, Telegram, Notes)
                ShareOptionRow(
                    icon = Icons.Default.Forum,
                    iconColor = colors.statusSuccess,
                    title = "Share as Plain Text",
                    subtitle = "Direct text sharing for WhatsApp, Slack, Telegram & Notes",
                    badge = "Instant",
                    onClick = {
                        onDismiss()
                        val text = TransactionFormatter.formatSingleTransaction(transaction)
                        ShareHandler.shareText(
                            title = "${transaction.request.method} ${transaction.request.path}",
                            content = text,
                            chooserTitle = "Share via WhatsApp / Apps"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Text File (.txt)
                ShareOptionRow(
                    icon = Icons.Default.Description,
                    iconColor = colors.methodGet,
                    title = "Share as Text File (.txt)",
                    subtitle = "Full formatted report as a .txt document attachment",
                    badge = ".TXT",
                    onClick = {
                        onDismiss()
                        val text = TransactionFormatter.formatSingleTransaction(transaction)
                        val safePath = transaction.request.path.replace("/", "_").take(20)
                        ShareHandler.shareFile(
                            fileName = "kourier_${transaction.request.method}${safePath}_${transaction.id}.txt",
                            content = text,
                            mimeType = "text/plain",
                            chooserTitle = "Share .txt Report"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 3: cURL Command
                ShareOptionRow(
                    icon = Icons.Default.Code,
                    iconColor = colors.methodPut,
                    title = "Share as cURL Command",
                    subtitle = "Executable terminal-ready cURL command snippet",
                    badge = "cURL",
                    onClick = {
                        onDismiss()
                        val curl = CurlGenerator.generate(transaction.request)
                        ShareHandler.shareText(
                            title = "cURL: ${transaction.request.method} ${transaction.request.path}",
                            content = curl,
                            chooserTitle = "Share cURL Command"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 4: HAR File (.har)
                ShareOptionRow(
                    icon = Icons.Default.FolderZip,
                    iconColor = colors.methodPatch,
                    title = "Share as HAR File (.har)",
                    subtitle = "HTTP Archive for Proxyman, Charles, Postman & DevTools",
                    badge = ".HAR",
                    onClick = {
                        onDismiss()
                        val harJson = HarExporter.export(listOf(transaction))
                        ShareHandler.shareFile(
                            fileName = "transaction_${transaction.id}.har",
                            content = harJson,
                            mimeType = "application/json",
                            chooserTitle = "Share .har Archive"
                        )
                    }
                )
            }
        }
        }
    }
}

@Composable
fun BatchExportDialog(
    transactions: List<HttpTransaction>,
    onDismiss: () -> Unit
) {
    val currentColors = LocalKourierColors.current

    Dialog(onDismissRequest = onDismiss) {
        KourierTheme(darkTheme = currentColors.isDark) {
            val colors = LocalKourierColors.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Network Telemetry",
                            style = KourierTypography.titleLarge,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${transactions.size} recorded network requests",
                            style = KourierTypography.caption,
                            color = colors.textMuted
                        )
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
                Spacer(modifier = Modifier.height(12.dp))

                // Option 1: HAR Archive (.har)
                ShareOptionRow(
                    icon = Icons.Default.FolderZip,
                    iconColor = colors.methodGet,
                    title = "Export as HAR Archive (.har)",
                    subtitle = "Standard HTTP Archive for Proxyman, Postman & DevTools",
                    badge = ".HAR",
                    onClick = {
                        onDismiss()
                        val harJson = HarExporter.export(transactions)
                        ShareHandler.shareFile(
                            fileName = "Kourier_Export.har",
                            content = harJson,
                            mimeType = "application/json",
                            chooserTitle = "Export .har Archive"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Full Report Text File (.txt)
                ShareOptionRow(
                    icon = Icons.Default.Description,
                    iconColor = colors.statusSuccess,
                    title = "Export as Text File (.txt)",
                    subtitle = "Consolidated full report document of all transactions",
                    badge = ".TXT",
                    onClick = {
                        onDismiss()
                        val plainText = HarExporter.exportAsPlainText(transactions)
                        ShareHandler.shareFile(
                            fileName = "Kourier_Export.txt",
                            content = plainText,
                            mimeType = "text/plain",
                            chooserTitle = "Export .txt Report"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 3: Plain Text Summary
                ShareOptionRow(
                    icon = Icons.Default.Forum,
                    iconColor = colors.methodPut,
                    title = "Share Summary as Plain Text",
                    subtitle = "Direct text sharing formatted for WhatsApp, Slack & Chat",
                    badge = "Instant",
                    onClick = {
                        onDismiss()
                        val plainText = HarExporter.exportAsPlainText(transactions)
                        ShareHandler.shareText(
                            title = "Kourier Network Report (${transactions.size} requests)",
                            content = plainText,
                            chooserTitle = "Share via WhatsApp / Apps"
                        )
                    }
                )
            }
        }
        }
    }
}

@Composable
private fun ShareOptionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {
    val colors = LocalKourierColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = KourierTypography.label,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(iconColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badge,
                            style = KourierTypography.badgeSmall,
                            color = iconColor
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
