package dev.shushant.kourier.core.storage

import dev.shushant.kourier.core.model.HttpTransaction
import kotlinx.coroutines.flow.Flow

interface KourierStorage {
    suspend fun insertOrUpdate(transaction: HttpTransaction)
    suspend fun getTransaction(id: String): HttpTransaction?
    fun observeTransactions(): Flow<List<HttpTransaction>>
    suspend fun getAllTransactions(): List<HttpTransaction>
    suspend fun clearAll()
    suspend fun deleteOlderThan(timestampMs: Long)
    suspend fun trimToCount(maxCount: Int)
    fun searchTransactions(
        query: String = "",
        statusFilter: String? = null,
        methodFilter: String? = null
    ): Flow<List<HttpTransaction>>
}
