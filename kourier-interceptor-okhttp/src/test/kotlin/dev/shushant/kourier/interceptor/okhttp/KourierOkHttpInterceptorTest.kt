package dev.shushant.kourier.interceptor.okhttp

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.ResilienceRuntime
import dev.shushant.kourier.scenarios.*
import kotlinx.coroutines.runBlocking
import java.net.SocketTimeoutException
import java.net.SocketException
import kotlin.test.assertFailsWith
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
        runBlocking { ResilienceRuntime.engine.disable() }
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
        runBlocking { ResilienceRuntime.engine.disable() }
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

        val transactions = kotlinx.coroutines.runBlocking { KourierCore.flush(); storage.getAllTransactions() }
        assertEquals(1, transactions.size)

        val tx = transactions.first()
        assertEquals("POST", tx.request.method)
        assertEquals(200, tx.response?.statusCode)
        assertTrue(tx.request.headers.any { it.name == "Authorization" && it.value == "••••••••" })
        assertTrue(tx.request.body?.contains("\"password\":\"••••••••\"") == true)
        assertFalse(tx.request.body?.contains("secret123") == true)
        assertEquals("{\"status\":\"success\"}", tx.response?.body)
    }

    private fun activate(action: ScenarioAction, first: Int? = null) = runBlocking {
        val scenario = Scenario("fault", "Fault", listOf(ScenarioRule("rule", RequestMatcher("GET", "/test", "api"), action, occurrence = Occurrence(first = first))),
            listOf(ResponseFixture("fixture", "application/json", "{\"ok\":false}")))
        assertEquals(emptyList(), ResilienceRuntime.engine.load(scenario, mapOf("api" to server.url("/").toString())))
        ResilienceRuntime.engine.enable()
    }

    @Test fun syntheticResponseAvoidsNetworkThenForwardsAfterExhaustion() {
        activate(ScenarioAction.HttpResponse(503, "fixture"), first = 1)
        server.enqueue(MockResponse().setResponseCode(200).setBody("real"))
        val request = Request.Builder().url(server.url("/test")).build()
        client.newCall(request).execute().use { response ->
            assertEquals(503, response.code)
            assertEquals("{\"ok\":false}", response.body.string())
        }
        assertEquals(0, server.requestCount)
        client.newCall(request).execute().use { assertEquals("real", it.body.string()) }
        assertEquals(1, server.requestCount)
        val transactions = runBlocking { KourierCore.flush(); storage.getAllTransactions() }
        val injected = transactions.single { it.tags["resilience.source"] == "injected" }
        assertEquals("rule", injected.tags["resilience.ruleId"])
        assertEquals(0, injected.totalBytesSent.toInt())
    }

    @Test fun injectedTransportErrorsAreNativeAndRecorded() {
        for (action in listOf(ScenarioAction.Timeout, ScenarioAction.Disconnect)) {
            activate(action)
            val request = Request.Builder().url(server.url("/test")).build()
            if (action == ScenarioAction.Timeout) assertFailsWith<SocketTimeoutException> { client.newCall(request).execute() }
            else assertFailsWith<SocketException> { client.newCall(request).execute() }
        }
        assertEquals(0, server.requestCount)
        val transactions = runBlocking { KourierCore.flush(); storage.getAllTransactions() }
        assertEquals(2, transactions.size)
        assertTrue(transactions.all { it.error != null && it.tags["resilience.source"] == "injected" })
    }

    @Test fun disabledScenarioLeavesRealResponseUnchanged() {
        activate(ScenarioAction.HttpResponse(503))
        runBlocking { ResilienceRuntime.engine.disable() }
        server.enqueue(MockResponse().setResponseCode(200).setBody("untouched"))
        client.newCall(Request.Builder().url(server.url("/test")).build()).execute().use {
            assertEquals(200, it.code); assertEquals("untouched", it.body.string())
        }
        assertEquals(1, server.requestCount)
    }
}
