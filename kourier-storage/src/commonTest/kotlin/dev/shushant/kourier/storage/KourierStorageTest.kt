package dev.shushant.kourier.storage

import dev.shushant.kourier.core.model.HttpRequest
import dev.shushant.kourier.core.model.HttpHeader
import dev.shushant.kourier.core.model.HttpResponse
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TransactionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KourierStorageTest {

    @Test
    fun testStorageInsertAndRetrieve() = runTest {
        val storage = InMemoryKourierStorage()
        val tx = HttpTransaction(
            id = "tx-1",
            timestamp = 1000L,
            request = HttpRequest("https://example.com/api/test", "GET"),
            response = HttpResponse(200, "OK"),
            status = TransactionStatus.SUCCESS
        )

        storage.insertOrUpdate(tx)
        val retrieved = storage.getTransaction("tx-1")
        assertNotNull(retrieved)
        assertEquals("tx-1", retrieved.id)
        assertEquals("GET", retrieved.request.method)

        val list = storage.observeTransactions().first()
        assertEquals(1, list.size)
    }

    @Test
    fun testStorageTrimToCount() = runTest {
        val storage = InMemoryKourierStorage()
        for (i in 1..10) {
            storage.insertOrUpdate(
                HttpTransaction(
                    id = "tx-$i",
                    timestamp = i * 100L,
                    request = HttpRequest("https://example.com/api/$i", "GET")
                )
            )
        }

        storage.trimToCount(5)
        val list = storage.getAllTransactions()
        assertEquals(5, list.size)
        // Most recent should be retained
        assertEquals("tx-10", list.first().id)
    }

    @Test
    fun testStorageSearch() = runTest {
        val storage = InMemoryKourierStorage()
        storage.insertOrUpdate(
            HttpTransaction(
                id = "tx-get",
                timestamp = 100L,
                request = HttpRequest("https://api.github.com/users", "GET"),
                response = HttpResponse(200, "OK")
            )
        )
        storage.insertOrUpdate(
            HttpTransaction(
                id = "tx-post",
                timestamp = 200L,
                request = HttpRequest("https://api.stripe.com/charges", "POST"),
                response = HttpResponse(400, "Bad Request")
            )
        )

        val filteredByMethod = storage.searchTransactions(methodFilter = "POST").first()
        assertEquals(1, filteredByMethod.size)
        assertEquals("POST", filteredByMethod.first().request.method)

        val filteredByErrors = storage.searchTransactions(statusFilter = "ERRORS").first()
        assertEquals(1, filteredByErrors.size)
        assertEquals(400, filteredByErrors.first().response?.statusCode)
    }

    @Test
    fun searchIncludesQueryAndHeaders() = runTest {
        val storage = InMemoryKourierStorage()
        storage.insertOrUpdate(
            HttpTransaction(
                id = "searchable",
                timestamp = 100L,
                request = HttpRequest(
                    url = "https://example.com/items?tenant=acme",
                    method = "GET",
                    headers = listOf(HttpHeader("X-Trace-Id", "trace-123"))
                ),
                response = HttpResponse(
                    statusCode = 200,
                    headers = listOf(HttpHeader("X-Region", "ap-south"))
                )
            )
        )

        assertEquals("searchable", storage.searchTransactions(query = "tenant=acme").first().single().id)
        assertEquals("searchable", storage.searchTransactions(query = "trace-123").first().single().id)
        assertEquals("searchable", storage.searchTransactions(query = "ap-south").first().single().id)
    }
}
