package dev.shushant.kourier.interceptor.ktor

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.ResilienceRuntime
import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.scenarios.*
import dev.shushant.kourier.storage.InMemoryKourierStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class KourierKtorScenarioTest {
    @BeforeTest fun before() = runBlocking { ResilienceRuntime.engine.disable() }
    @AfterTest fun after() = runBlocking { ResilienceRuntime.engine.disable() }

    private suspend fun activate(action: ScenarioAction, first: Int? = null, operation: String? = null) {
        val match = RequestMatcher(if (operation == null) "GET" else "POST", "/test", "api", graphqlOperation = operation)
        val scenario = Scenario("fault", "Fault", listOf(ScenarioRule("rule", match, action, occurrence = Occurrence(first = first))),
            listOf(ResponseFixture("fixture", "application/json", "{\"error\":\"unavailable\"}")))
        assertEquals(emptyList(), ResilienceRuntime.engine.load(scenario, mapOf("api" to "https://api.example")))
        assertTrue(ResilienceRuntime.engine.enable())
    }

    @Test fun syntheticResponseSkipsEngineAndPreservesBody() = runTest {
        val storage = InMemoryKourierStorage(); KourierCore.initialize(KourierConfig(), storage)
        var sent = 0
        val client = HttpClient(MockEngine { sent++; respond("real", HttpStatusCode.OK) }) { install(KourierKtorPlugin) }
        try {
            activate(ScenarioAction.HttpResponse(503, "fixture"), first = 1)
            val synthetic = client.get("https://api.example/test")
            assertEquals(503, synthetic.status.value)
            assertEquals("{\"error\":\"unavailable\"}", synthetic.bodyAsText())
            assertEquals(0, sent)
            assertEquals("real", client.get("https://api.example/test").bodyAsText())
            assertEquals(1, sent)
            KourierCore.flush()
            val injected = storage.getAllTransactions().single { it.tags["resilience.source"] == "injected" }
            assertEquals(503, injected.response?.statusCode)
            assertEquals("rule", injected.tags["resilience.ruleId"])
            assertEquals(0, injected.totalBytesSent.toInt())
        } finally { client.close() }
    }

    @Test fun transportErrorsUseNativePathsAndRecordEvidence() = runTest {
        val storage = InMemoryKourierStorage(); KourierCore.initialize(KourierConfig(), storage)
        var sent = 0
        val client = HttpClient(MockEngine { sent++; respond("unexpected") }) { install(KourierKtorPlugin) }
        try {
            activate(ScenarioAction.Timeout)
            assertFailsWith<HttpRequestTimeoutException> { client.get("https://api.example/test") }
            activate(ScenarioAction.Disconnect)
            assertFailsWith<IOException> { client.get("https://api.example/test") }
            assertEquals(0, sent)
            KourierCore.flush()
            val transactions = storage.getAllTransactions()
            assertEquals(2, transactions.size)
            assertTrue(transactions.all { it.error != null && it.tags["resilience.source"] == "injected" })
        } finally { client.close() }
    }

    @Test fun disabledAndOtherGraphqlOperationsForwardUnchanged() = runTest {
        KourierCore.initialize(KourierConfig(), InMemoryKourierStorage())
        var sent = 0
        val client = HttpClient(MockEngine { sent++; respond("real") }) { install(KourierKtorPlugin) }
        try {
            activate(ScenarioAction.HttpResponse(503), operation = "StartSession")
            assertEquals("real", client.post("https://api.example/test") { setBody("{\"operationName\":\"Other\"}") }.bodyAsText())
            assertEquals(503, client.post("https://api.example/test") { setBody("{\"operationName\":\"StartSession\"}") }.status.value)
            ResilienceRuntime.engine.disable()
            assertEquals("real", client.post("https://api.example/test") { setBody("{\"operationName\":\"StartSession\"}") }.bodyAsText())
            assertEquals(2, sent)
        } finally { client.close() }
    }
    @Test fun syntheticResponsesRespectHostValidation() = runTest {
        val storage = InMemoryKourierStorage(); KourierCore.initialize(KourierConfig(), storage)
        var sent = 0
        val client = HttpClient(MockEngine { sent++; respond("unexpected") }) {
            expectSuccess = true
            install(KourierKtorPlugin)
        }
        try {
            activate(ScenarioAction.HttpResponse(503, "fixture"))
            assertFailsWith<ServerResponseException> { client.get("https://api.example/test") }
            assertEquals(0, sent)
            KourierCore.flush()
            val transaction = storage.getAllTransactions().single()
            assertEquals(503, transaction.response?.statusCode)
            assertEquals("rule", transaction.tags["resilience.ruleId"])
        } finally { client.close() }
    }

}
