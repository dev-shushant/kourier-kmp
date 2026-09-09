package dev.shushant.kourier.ui.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.storage.TransactionFilter
import kotlinx.coroutines.flow.collectLatest

class InspectorStateHolder {
    var rawTransactions by mutableStateOf<List<HttpTransaction>>(emptyList())
    var filteredTransactions by mutableStateOf<List<HttpTransaction>>(emptyList())
    var selectedTransaction by mutableStateOf<HttpTransaction?>(null)
    var searchQuery by mutableStateOf("")
    var selectedStatus by mutableStateOf("ALL")
    var selectedMethod by mutableStateOf("ALL")

    fun refresh(transactions: List<HttpTransaction>) {
        rawTransactions = transactions
        applyFilters()
        val currentId = selectedTransaction?.id
        if (currentId != null) {
            selectedTransaction = transactions.firstOrNull { it.id == currentId }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }

    fun updateStatusFilter(status: String) {
        selectedStatus = status
        applyFilters()
    }

    fun updateMethodFilter(method: String) {
        selectedMethod = method
        applyFilters()
    }

    fun selectTransaction(transaction: HttpTransaction?) {
        selectedTransaction = transaction
    }

    private fun applyFilters() {
        val filter = TransactionFilter(
            query = searchQuery,
            status = selectedStatus,
            method = selectedMethod
        )
        filteredTransactions = filter.filter(rawTransactions)
    }
}

@Composable
fun rememberInspectorState(): InspectorStateHolder {
    val state = remember { InspectorStateHolder() }

    LaunchedEffect(Unit) {
        val initial = KourierCore.storage?.getAllTransactions() ?: emptyList()
        state.refresh(initial)

        KourierCore.eventBus.events.collectLatest { _ ->
            val updated = KourierCore.storage?.getAllTransactions() ?: emptyList()
            state.refresh(updated)
        }
    }

    return state
}
