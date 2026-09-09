package dev.shushant.kourier.core

import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.core.model.HttpRequest
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TransactionStatus
import dev.shushant.kourier.core.storage.KourierStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KourierCoreRuntimeTest {
    @AfterTest
    fun tearDown() {
        KourierCore.resetForTests()
    }

    @Test
    fun flushWaitsForStartedAndCompletedWritesInOrder() = runTest {
        val storage = RecordingStorage()
        KourierCore.initialize(KourierConfig(), storage)

        KourierCore.recordRequestStarted(transaction("call", TransactionStatus.PENDING))
        KourierCore.recordTransactionCompleted(transaction("call", TransactionStatus.SUCCESS))
        KourierCore.flush()

        assertEquals(
            listOf(TransactionStatus.PENDING, TransactionStatus.SUCCESS),
            storage.writes.map { it.status }
        )
        assertEquals(TransactionStatus.SUCCESS, storage.current["call"]?.status)
    }

    @Test
    fun reinitializeRoutesNewEventsOnlyToNewStorage() = runTest {
        val first = RecordingStorage()
        val second = RecordingStorage()
        KourierCore.initialize(KourierConfig(), first)
        KourierCore.recordRequestStarted(transaction("first", TransactionStatus.PENDING))
        KourierCore.flush()

        KourierCore.initialize(KourierConfig(), second)
        KourierCore.recordRequestStarted(transaction("second", TransactionStatus.PENDING))
        KourierCore.flush()

        assertEquals(listOf("first"), first.writes.map { it.id })
        assertEquals(listOf("second"), second.writes.map { it.id })
    }

    @Test
    fun storageFailureDoesNotPreventLaterEvents() = runTest {
        val storage = RecordingStorage(failFirstWrite = true)
        KourierCore.initialize(KourierConfig(), storage)

        KourierCore.recordRequestStarted(transaction("first", TransactionStatus.PENDING))
        KourierCore.recordRequestStarted(transaction("second", TransactionStatus.PENDING))
        KourierCore.flush()

        assertEquals(listOf("second"), storage.writes.map { it.id })
    }

    private fun transaction(id: String, status: TransactionStatus) = HttpTransaction(
        id = id,
        timestamp = 1L,
        request = HttpRequest(url = "https://example.test/$id", method = "GET"),
        status = status
    )

    private class RecordingStorage(
        private var failFirstWrite: Boolean = false
    ) : KourierStorage {
        val writes = mutableListOf<HttpTransaction>()
        val current = mutableMapOf<String, HttpTransaction>()
        private val state = MutableStateFlow<List<HttpTransaction>>(emptyList())

        override suspend fun insertOrUpdate(transaction: HttpTransaction) {
            if (failFirstWrite) {
                failFirstWrite = false
                error("synthetic persistence failure")
            }
            writes += transaction
            current[transaction.id] = transaction
            state.value = current.values.toList()
        }

        override suspend fun getTransaction(id: String) = current[id]
        override fun observeTransactions(): Flow<List<HttpTransaction>> = state
        override suspend fun getAllTransactions() = current.values.toList()
        override suspend fun clearAll() { current.clear(); state.value = emptyList() }
        override suspend fun deleteOlderThan(timestampMs: Long) = Unit
        override suspend fun trimToCount(maxCount: Int) = Unit
        override fun searchTransactions(query: String, statusFilter: String?, methodFilter: String?) = state
    }
}
