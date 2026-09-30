package dev.shushant.kourier.interceptor.ktor

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.ResilienceRuntime
import dev.shushant.kourier.scenarios.GraphqlInspector
import dev.shushant.kourier.scenarios.ScenarioAction
import io.ktor.client.call.HttpClientCall
import io.ktor.client.request.HttpResponseData
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.InternalAPI
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import dev.shushant.kourier.core.model.HttpRequest as KourierHttpRequest
import dev.shushant.kourier.core.model.HttpResponse as KourierHttpResponse
import dev.shushant.kourier.core.model.HttpTimings
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TransactionStatus
import dev.shushant.kourier.core.platform.PlatformUtils
import io.ktor.client.call.save
import dev.shushant.kourier.core.model.ErrorPayload
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.util.AttributeKey
import io.ktor.util.flattenForEach
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray

class KourierKtorPluginConfig {
    var captureCallStack: Boolean = true
    var maxCallStackDepth: Int = 15
}

private val KourierGraphqlOperationKey = AttributeKey<String>("KourierGraphqlOperation")

private val KourierTransactionIdKey = AttributeKey<String>("KourierTransactionId")
private val KourierStartNsKey = AttributeKey<Long>("KourierStartNs")
private val KourierInitialTxKey = AttributeKey<HttpTransaction>("KourierInitialTx")

@OptIn(InternalAPI::class)
val KourierKtorPlugin = createClientPlugin("KourierKtorPlugin", ::KourierKtorPluginConfig) {
    val pluginConfig = pluginConfig
    val hostClient = client

    onRequest { request, content ->
        val transactionId = PlatformUtils.randomUuid()
        val startNs = PlatformUtils.nanoTime()
        val startEpochMs = PlatformUtils.currentTimeMillis()

        request.attributes.put(KourierTransactionIdKey, transactionId)
        request.attributes.put(KourierStartNsKey, startNs)

        val config = KourierCore.config
        val dataMasker = config.dataMasker
        val truncator = KourierCore.truncator

        val callStack = if (pluginConfig.captureCallStack && config.captureCallStack) {
            PlatformUtils.captureSanitizedCallStack(pluginConfig.maxCallStackDepth)
        } else {
            emptyList()
        }

        val actualContent = if (content is OutgoingContent.NoContent) request.body else content
        // Extract request body if text or known outgoing content
        val (requestBodyString, isTruncated, bodyBytes) = when (actualContent) {
            is TextContent -> {
                val text = actualContent.text
                val truncResult = truncator.truncateString(text)
                Triple(truncResult.content, truncResult.isTruncated, truncResult.originalSizeBytes)
            }
            is OutgoingContent.ByteArrayContent -> {
                val bytes = actualContent.bytes()
                val truncResult = truncator.truncateBytes(bytes)
                Triple(truncResult.content, truncResult.isTruncated, truncResult.originalSizeBytes)
            }
            is String -> {
                val truncResult = truncator.truncateString(actualContent)
                Triple(truncResult.content, truncResult.isTruncated, truncResult.originalSizeBytes)
            }
            is ByteArray -> {
                val truncResult = truncator.truncateBytes(actualContent)
                Triple(truncResult.content, truncResult.isTruncated, truncResult.originalSizeBytes)
            }
            else -> {
                val text = actualContent.toString()
                if (actualContent !is OutgoingContent.NoContent && actualContent != io.ktor.client.utils.EmptyContent) {
                    val truncResult = truncator.truncateString(text)
                    Triple(truncResult.content, truncResult.isTruncated, truncResult.originalSizeBytes)
                } else {
                    Triple(null, false, 0L)
                }
            }
        }

        if (!isTruncated) GraphqlInspector.request(requestBodyString).operationName?.let {
            request.attributes.put(KourierGraphqlOperationKey, it)
        }
        val maskedRequestBody = dataMasker.maskPayload(requestBodyString)

        val rawHeaders = mutableListOf<Pair<String, String>>()
        request.headers.entries().forEach { (k, values) -> values.forEach { v -> rawHeaders.add(k to v) } }
        val maskedHeaders = dataMasker.maskHeaders(rawHeaders)
        val maskedUrl = dataMasker.maskUrl(request.url.buildString())

        val httpRequest = KourierHttpRequest(
            url = maskedUrl,
            method = request.method.value,
            headers = maskedHeaders,
            body = maskedRequestBody,
            contentType = request.headers["Content-Type"],
            contentLength = bodyBytes,
            isTruncated = isTruncated,
            uncompressedSizeBytes = bodyBytes
        )

        val initialTx = HttpTransaction(
            id = transactionId,
            timestamp = startEpochMs,
            request = httpRequest,
            status = TransactionStatus.PENDING,
            timings = HttpTimings(requestStartNs = startNs),
            callStack = callStack,
            totalBytesSent = bodyBytes
        )

        request.attributes.put(KourierInitialTxKey, initialTx)
        KourierCore.recordRequestStarted(initialTx)
    }

    on(Send) { request ->
        try {
            val engine = ResilienceRuntime.engine
            val rawHeaders = request.headers.entries().flatMap { (name, values) -> values.map { name to it } }
            val decision = if (engine.isActive()) engine.decideUrl(
                request.url.buildString(), request.method.value, rawHeaders,
                request.attributes.getOrNull(KourierGraphqlOperationKey)
            ) else null
            if (decision == null || !engine.awaitDelay(decision)) {
                proceed(request)
            } else {
                request.attributes.getOrNull(KourierInitialTxKey)?.let { initial ->
                    val updated = initial.copy(
                        tags = ResilienceRuntime.evidence(decision, "ktor"),
                        totalBytesSent = if (decision.action == ScenarioAction.Forward) initial.totalBytesSent else 0
                    )
                    request.attributes.put(KourierInitialTxKey, updated)
                    KourierCore.recordTransactionUpdated(updated)
                }
                when (val action = decision.action) {
                    ScenarioAction.Forward -> proceed(request)
                    ScenarioAction.Timeout -> throw HttpRequestTimeoutException(request)
                    ScenarioAction.Disconnect -> throw IOException("Injected disconnect")
                    is ScenarioAction.HttpResponse -> {
                        val fixture = decision.fixture
                        val body = (fixture?.body ?: "").encodeToByteArray()
                        val headers = Headers.build {
                            fixture?.contentType?.let { append(HttpHeaders.ContentType, it) }
                            append(HttpHeaders.ContentLength, body.size.toString())
                        }
                        val callContext = currentCoroutineContext() + Job(request.executionContext)
                        val syntheticCall = HttpClientCall(hostClient, request.build(), HttpResponseData(
                            HttpStatusCode(action.status, "Injected response"), GMTDate(), headers,
                            HttpProtocolVersion.HTTP_1_1, ByteReadChannel(body), callContext
                        ))
                        hostClient.receivePipeline.execute(Unit, syntheticCall.response).call
                    }
                }
            }
        } catch (cause: Throwable) {
            val initialTx = request.attributes.getOrNull(KourierInitialTxKey)
            if (initialTx != null) {
                val failureNs = PlatformUtils.nanoTime()
                val startNs = request.attributes.getOrNull(KourierStartNsKey) ?: initialTx.timings.requestStartNs
                val durationMs = (failureNs - startNs) / 1_000_000L
                val errorPayload = ErrorPayload(
                    message = cause.message ?: cause.toString(),
                    exceptionClass = cause::class.simpleName ?: "Throwable",
                    stackTrace = cause.stackTraceToString(),
                    timestamp = PlatformUtils.currentTimeMillis()
                )
                val failedTx = initialTx.copy(
                    status = TransactionStatus.FAILED,
                    error = errorPayload,
                    timings = initialTx.timings.copy(
                        requestEndNs = failureNs,
                        durationMs = durationMs
                    )
                )
                KourierCore.recordTransactionCompleted(failedTx)
            }
            throw cause
        }
    }

    onResponse { response ->
        val initialTx = response.call.request.attributes.getOrNull(KourierInitialTxKey) ?: return@onResponse
        val startNs = response.call.request.attributes.getOrNull(KourierStartNsKey) ?: PlatformUtils.nanoTime()
        val endNs = PlatformUtils.nanoTime()
        val durationMs = (endNs - startNs) / 1_000_000L

        val config = KourierCore.config
        val dataMasker = config.dataMasker
        val truncator = KourierCore.truncator

        val rawHeaders = mutableListOf<Pair<String, String>>()
        response.headers.flattenForEach { k, v -> rawHeaders.add(k to v) }
        val maskedHeaders = dataMasker.maskHeaders(rawHeaders)

        val (responseBodyString, isTruncated, bodyBytes) = try {
            val savedCall = response.call.save()
            val text = savedCall.response.bodyAsText()
            val truncResult = truncator.truncateString(text)
            Triple(truncResult.content, truncResult.isTruncated, truncResult.originalSizeBytes)
        } catch (_: Throwable) {
            val len = response.headers["Content-Length"]?.toLongOrNull() ?: 0L
            Triple(null, false, len)
        }

        val maskedResponseBody = dataMasker.maskPayload(responseBodyString)
        val contentType = response.headers["Content-Type"]

        val responseModel = KourierHttpResponse(
            statusCode = response.status.value,
            message = response.status.description,
            headers = maskedHeaders,
            body = maskedResponseBody,
            contentType = contentType,
            contentLength = bodyBytes,
            isTruncated = isTruncated,
            uncompressedSizeBytes = bodyBytes
        )

        val completedTx = initialTx.copy(
            response = responseModel,
            status = if (response.status.value in 200..299) TransactionStatus.SUCCESS else TransactionStatus.FAILED,
            timings = initialTx.timings.copy(
                responseStartNs = endNs,
                responseEndNs = endNs,
                durationMs = durationMs
            ),
            protocol = response.version.name,
            totalBytesReceived = bodyBytes
        )

        response.call.request.attributes.put(KourierInitialTxKey, completedTx)
        KourierCore.recordTransactionCompleted(completedTx)
    }
}

