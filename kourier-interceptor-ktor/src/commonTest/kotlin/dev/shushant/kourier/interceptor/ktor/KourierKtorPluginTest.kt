package dev.shushant.kourier.interceptor.ktor

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.storage.InMemoryKourierStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KourierKtorPluginTest {

    @Test
    fun testKtorPluginInterception() = runTest {
        val storage = InMemoryKourierStorage()
        KourierCore.initialize(
            config = KourierConfig.Builder()
                .redactHeaders("Authorization")
                .build(),
            storage = storage
        )

        val mockEngine = MockEngine { request ->
            respond(
                content = """{"status":"ok","count":42}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(KourierKtorPlugin)
        }

        val response = client.get("https://api.example.com/v1/data") {
            header("Authorization", "Bearer my-secret-jwt")
            header("Accept", "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val downstreamBody = response.bodyAsText()
        assertTrue(downstreamBody.contains(""""status":"ok""""))

        KourierCore.flush()

        val transactions = storage.getAllTransactions()
        assertEquals(1, transactions.size)

        val tx = transactions.first()
        assertEquals("GET", tx.request.method)
        assertTrue(tx.request.headers.any { it.name == "Authorization" && it.value == "••••••••" })
        assertEquals(200, tx.response?.statusCode)
        assertNotNull(tx.response?.body)
        assertTrue(tx.response!!.body!!.contains(""""status":"ok""""))
    }

    @Test
    fun testKtorPluginPostWithMaskingAndErrors() = runTest {
        val storage = InMemoryKourierStorage()
        KourierCore.initialize(
            config = KourierConfig.Builder()
                .redactPayloadKeys("password")
                .build(),
            storage = storage
        )

        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error":"Forbidden","code":403}""",
                status = HttpStatusCode.Forbidden,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(KourierKtorPlugin)
        }

        val response = client.post("https://api.example.com/v1/login") {
            header("Content-Type", "application/json")
            setBody("""{"username":"test","password":"secretPassword123"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)

        KourierCore.flush()

        val transactions = storage.getAllTransactions()
        assertEquals(1, transactions.size)

        val tx = transactions.first()
        assertEquals("POST", tx.request.method)
        assertTrue(tx.request.body!!.contains(""""password":"••••••••""""))
        assertEquals(403, tx.response?.statusCode)
        assertTrue(tx.response!!.body!!.contains(""""error":"Forbidden""""))
    }
}

