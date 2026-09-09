package dev.shushant.kourier.ui.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors

@Composable
fun AdaptiveLayoutContainer(
    transactions: List<HttpTransaction>,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current
    var selectedTransactionId by remember { mutableStateOf<String?>(null) }
    val selectedTransaction = transactions.firstOrNull { it.id == selectedTransactionId }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val constraints = this
        val isWideScreen = constraints.maxWidth >= 720.dp

        if (isWideScreen) {
            // Landscape / Tablet / Foldable: Split-pane master-detail
            Row(modifier = Modifier.fillMaxSize()) {
                // Left pane: Master list
                val listWidth = (constraints.maxWidth * 0.38f).coerceIn(300.dp, 450.dp)
                TransactionListScreen(
                    transactions = transactions,
                    selectedTransactionId = selectedTransactionId,
                    onSelectTransaction = { selectedTransactionId = it.id },
                    onClearAll = {
                        onClearAll()
                        selectedTransactionId = null
                    },
                    onClose = onClose,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    selectedStatus = selectedStatus,
                    onStatusSelected = onStatusSelected,
                    selectedMethod = selectedMethod,
                    onMethodSelected = onMethodSelected,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    modifier = Modifier.width(listWidth).fillMaxHeight()
                )

                // Split border
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colors.border)
                )

                // Right pane: Detail Inspector
                if (selectedTransaction != null) {
                    TransactionDetailScreen(
                        transaction = selectedTransaction,
                        onBack = null,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SELECT A TRANSACTION TO INSPECT",
                            style = KourierTypography.subtitle,
                            color = colors.textMuted
                        )
                    }
                }
            }
        } else {
            // Portrait Phone: Single screen with push-pop navigation
            if (selectedTransaction != null) {
                TransactionDetailScreen(
                    transaction = selectedTransaction,
                    onBack = { selectedTransactionId = null },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme
                )
            } else {
                TransactionListScreen(
                    transactions = transactions,
                    selectedTransactionId = null,
                    onSelectTransaction = { selectedTransactionId = it.id },
                    onClearAll = onClearAll,
                    onClose = onClose,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    selectedStatus = selectedStatus,
                    onStatusSelected = onStatusSelected,
                    selectedMethod = selectedMethod,
                    onMethodSelected = onMethodSelected,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme
                )
            }
        }
    }
}

