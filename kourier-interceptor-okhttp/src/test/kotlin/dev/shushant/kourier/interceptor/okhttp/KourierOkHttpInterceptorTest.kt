package dev.shushant.kourier.interceptor.okhttp

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.storage.InMemoryKourierStorage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KourierOkHttpInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var storage: InMemoryKourierStorage

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        storage = InMemoryKourierStorage()
        KourierCore.initialize(
            config = KourierConfig.Builder()
                .redactHeaders("Authorization")
                .redactPayloadKeys("password")
                .build(),
            storage = storage
        )

        client = OkHttpClient.Builder()
            .addInterceptor(KourierOkHttpInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testSuccessfulRequestInterception() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"success\"}")
        )

        val request = Request.Builder()
            .url(server.url("/api/v1/test"))
            .header("Authorization", "Bearer sensitive-token")
            .post("{\"username\":\"john\",\"password\":\"secret123\"}".toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)
        assertEquals("{\"status\":\"success\"}", response.body.string())

        val transactions = kotlinx.coroutines.runBlocking { storage.getAllTransactions() }
        assertEquals(1, transactions.size)

        val tx = transactions.first()
        assertEquals("POST", tx.request.method)
        assertEquals(200, tx.response?.statusCode)
        assertTrue(tx.request.headers.any { it.name == "Authorization" && it.value == "••••••••" })
        assertTrue(tx.request.body?.contains("\"password\":\"••••••••\"") == true)
        assertFalse(tx.request.body?.contains("secret123") == true)
        assertEquals("{\"status\":\"success\"}", tx.response?.body)
    }
}
