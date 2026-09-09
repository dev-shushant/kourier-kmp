package dev.shushant.kourier.ui.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.ui.components.StickyTelemetryBottomBar
import dev.shushant.kourier.ui.theme.KourierTheme
import dev.shushant.kourier.ui.theme.LocalKourierColors

@Composable
fun KourierRootScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val systemInDark = isSystemInDarkTheme()
    var isDarkTheme by remember {
        mutableStateOf(KourierCore.config.darkTheme ?: systemInDark)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("ALL") }
    var selectedMethod by remember { mutableStateOf("ALL") }

    val storage = KourierCore.storage
    val transactionsFlow = remember(storage, searchQuery, selectedStatus, selectedMethod) {
        storage?.searchTransactions(
            query = searchQuery,
            statusFilter = selectedStatus,
            methodFilter = selectedMethod
        )
    }

    val transactions by (transactionsFlow?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<HttpTransaction>()) })

    val telemetry by KourierCore.eventBus.telemetry.collectAsState()

    KourierTheme(darkTheme = isDarkTheme) {
        val colors = LocalKourierColors.current
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AdaptiveLayoutContainer(
                    transactions = transactions,
                    onClearAll = { KourierCore.clearAll() },
                    onClose = onClose,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = it },
                    selectedMethod = selectedMethod,
                    onMethodSelected = { selectedMethod = it },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }

            // Sticky Bottom Telemetry Bar
            StickyTelemetryBottomBar(telemetry = telemetry)
        }
    }
}

