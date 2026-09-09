package dev.shushant.kourier.storage

import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.storage.KourierStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryKourierStorage(
    private val initialCapacity: Int = 500
) : KourierStorage {

    private val mutex = Mutex()
    private val transactions = mutableListOf<HttpTransaction>()
    private val _transactionsFlow = MutableStateFlow<List<HttpTransaction>>(emptyList())

    override suspend fun insertOrUpdate(transaction: HttpTransaction) {
        mutex.withLock {
            val existingIndex = transactions.indexOfFirst { it.id == transaction.id }
            if (existingIndex >= 0) {
                transactions[existingIndex] = transaction
            } else {
                transactions.add(0, transaction) // Add to top
            }
            _transactionsFlow.value = transactions.toList()
        }
    }

    override suspend fun getTransaction(id: String): HttpTransaction? {
        return mutex.withLock {
            transactions.firstOrNull { it.id == id }
        }
    }

    override fun observeTransactions(): Flow<List<HttpTransaction>> {
        return _transactionsFlow.asStateFlow()
    }

    override suspend fun getAllTransactions(): List<HttpTransaction> {
        return mutex.withLock { transactions.toList() }
    }

    override suspend fun clearAll() {
        mutex.withLock {
            transactions.clear()
            _transactionsFlow.value = emptyList()
        }
    }

    override suspend fun deleteOlderThan(timestampMs: Long) {
        mutex.withLock {
            transactions.removeAll { it.timestamp < timestampMs }
            _transactionsFlow.value = transactions.toList()
        }
    }

    override suspend fun trimToCount(maxCount: Int) {
        mutex.withLock {
            if (transactions.size > maxCount) {
                val toRemove = transactions.size - maxCount
                for (i in 0 until toRemove) {
                    if (transactions.isNotEmpty()) {
                        transactions.removeAt(transactions.lastIndex)
                    }
                }
                _transactionsFlow.value = transactions.toList()
            }
        }
    }

    override fun searchTransactions(
        query: String,
        statusFilter: String?,
        methodFilter: String?
    ): Flow<List<HttpTransaction>> {
        return _transactionsFlow.map { list ->
            list.filter { it.matches(query, statusFilter, methodFilter) }
        }
    }
}
