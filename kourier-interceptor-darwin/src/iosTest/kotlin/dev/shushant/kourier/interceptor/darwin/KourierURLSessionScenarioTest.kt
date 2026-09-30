package dev.shushant.kourier.interceptor.darwin

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.ResilienceRuntime
import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.scenarios.*
import dev.shushant.kourier.storage.InMemoryKourierStorage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.Foundation.*
import kotlin.coroutines.resume
import kotlin.test.*

@OptIn(ExperimentalForeignApi::class)
class KourierURLSessionScenarioTest {
    @BeforeTest fun before() = runBlocking { ResilienceRuntime.engine.disable() }
    @AfterTest fun after() = runBlocking { ResilienceRuntime.engine.disable() }

    private data class Result(val body: String?, val response: NSHTTPURLResponse?, val error: NSError?)

    private suspend fun request(): Result = withContext(Dispatchers.Default) {
        val config = KourierURLSessionConfiguration.enable(NSURLSessionConfiguration.ephemeralSessionConfiguration)
        val session = NSURLSession.sessionWithConfiguration(config)
        try {
            withTimeout(5000) {
                suspendCancellableCoroutine { continuation ->
                    val task = session.dataTaskWithURL(NSURL.URLWithString("https://fault.invalid/test")!!) { data, response, error ->
                        val body = data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() }
                        continuation.resume(Result(body, response as? NSHTTPURLResponse, error))
                    }
                    continuation.invokeOnCancellation { task.cancel() }
                    task.resume()
                }
            }
        } finally { session.invalidateAndCancel() }
    }

    private suspend fun activate(action: ScenarioAction) {
        val scenario = Scenario("ios-fault", "iOS fault", listOf(ScenarioRule("rule", RequestMatcher("GET", "/test", "api"), action)),
            listOf(ResponseFixture("fixture", "application/json", "{\"message\":\"Unavailable ✓\"}")))
        assertEquals(emptyList(), ResilienceRuntime.engine.load(scenario, mapOf("api" to "https://fault.invalid")))
        assertTrue(ResilienceRuntime.engine.enable())
    }

    @Test fun syntheticResponseUsesActualUrlSessionCallbacks() = runTest {
        val storage = InMemoryKourierStorage(); KourierCore.initialize(KourierConfig(), storage)
        activate(ScenarioAction.HttpResponse(503, "fixture"))
        val result = request()
        assertNull(result.error)
        assertEquals(503, result.response?.statusCode?.toInt())
        assertEquals("{\"message\":\"Unavailable ✓\"}", result.body)
        KourierCore.flush()
        val tx = storage.getAllTransactions().single()
        assertEquals(503, tx.response?.statusCode)
        assertEquals("urlsession", tx.tags["resilience.adapter"])
        assertEquals(0, tx.totalBytesSent.toInt())
    }

    @Test fun transportFaultsUseNativeNSErrorCodes() = runTest {
        val storage = InMemoryKourierStorage(); KourierCore.initialize(KourierConfig(), storage)
        for ((action, code) in listOf(ScenarioAction.Timeout to NSURLErrorTimedOut, ScenarioAction.Disconnect to NSURLErrorNetworkConnectionLost)) {
            activate(action)
            val result = request()
            assertEquals(NSURLErrorDomain, result.error?.domain)
            assertEquals(code, result.error?.code)
            assertNull(result.response)
        }
        KourierCore.flush()
        assertEquals(2, storage.getAllTransactions().size)
        assertTrue(storage.getAllTransactions().all { it.error != null && it.tags["resilience.source"] == "injected" })
    }
    @Test fun hostCancellationStopsPendingInjectedDelay() = runTest {
        val storage = InMemoryKourierStorage(); KourierCore.initialize(KourierConfig(), storage)
        val scenario = Scenario("cancel", "Cancel", listOf(ScenarioRule("rule", RequestMatcher("GET", "/test", "api"), ScenarioAction.HttpResponse(503), delayMs = 30_000)))
        ResilienceRuntime.engine.load(scenario, mapOf("api" to "https://fault.invalid")); ResilienceRuntime.engine.enable()
        val call = async(Dispatchers.Default) { request() }
        withContext(Dispatchers.Default) {
            withTimeout(3000) {
                while (storage.getAllTransactions().isEmpty()) delay(10)
            }
        }
        call.cancelAndJoin()
        withContext(Dispatchers.Default) {
            withTimeout(3000) {
                while (storage.getAllTransactions().single().error == null) delay(10)
            }
        }
        val tx = storage.getAllTransactions().single()
        assertEquals("Request cancelled", tx.error?.message)
        assertNull(tx.response)
        assertTrue(call.isCancelled)
    }

}
