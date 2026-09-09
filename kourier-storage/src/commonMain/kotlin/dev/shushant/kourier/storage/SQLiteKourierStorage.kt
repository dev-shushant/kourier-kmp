package dev.shushant.kourier.storage

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TransactionStatus
import dev.shushant.kourier.core.storage.KourierStorage
import dev.shushant.kourier.storage.db.KourierDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SQLiteKourierStorage(
    driver: SqlDriver
) : KourierStorage {

    private val database = KourierDatabase(driver)
    private val queries = database.kourierDatabaseQueries

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun insertOrUpdate(transaction: HttpTransaction): Unit = withContext(Dispatchers.IO) {
        val serialized = json.encodeToString(transaction)
        val resp = transaction.response
        val isFailed = if (transaction.status == TransactionStatus.FAILED ||
            (resp != null && resp.statusCode >= 400)
        ) 1L else 0L

        queries.insertOrReplace(
            id = transaction.id,
            timestamp = transaction.timestamp,
            method = transaction.request.method,
            url = transaction.request.url,
            host = transaction.request.host,
            path = transaction.request.path,
            statusCode = resp?.statusCode?.toLong(),
            durationMs = transaction.durationMs,
            isFailed = isFailed,
            serializedData = serialized
        )
    }

    override suspend fun getTransaction(id: String): HttpTransaction? = withContext(Dispatchers.IO) {
        val row = queries.selectById(id).executeAsOneOrNull() ?: return@withContext null
        try {
            json.decodeFromString<HttpTransaction>(row)
        } catch (_: Exception) {
            null
        }
    }

    override fun observeTransactions(): Flow<List<HttpTransaction>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.mapNotNull { raw ->
                    try {
                        json.decodeFromString<HttpTransaction>(raw)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
    }

    override suspend fun getAllTransactions(): List<HttpTransaction> = withContext(Dispatchers.IO) {
        queries.selectAll().executeAsList().mapNotNull { raw ->
            try {
                json.decodeFromString<HttpTransaction>(raw)
            } catch (_: Exception) {
                null
            }
        }
    }

    override suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        queries.deleteAll()
    }

    override suspend fun deleteOlderThan(timestampMs: Long): Unit = withContext(Dispatchers.IO) {
        queries.deleteOlderThan(timestampMs)
    }

    override suspend fun trimToCount(maxCount: Int): Unit = withContext(Dispatchers.IO) {
        queries.trimToCount(maxCount.toLong())
    }

    override fun searchTransactions(
        query: String,
        statusFilter: String?,
        methodFilter: String?
    ): Flow<List<HttpTransaction>> {
        return observeTransactions().map { list ->
            list.filter { it.matches(query, statusFilter, methodFilter) }
        }
    }
}
