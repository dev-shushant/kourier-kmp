package dev.shushant.kourier.interceptor.okhttp

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.model.CallStackElement
import dev.shushant.kourier.core.model.ErrorPayload
import dev.shushant.kourier.core.model.HttpRequest
import dev.shushant.kourier.core.model.HttpResponse
import dev.shushant.kourier.core.model.HttpTimings
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TransactionStatus
import dev.shushant.kourier.core.platform.PlatformUtils
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class KourierOkHttpInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val transactionId = PlatformUtils.randomUuid()
        val startNs = PlatformUtils.nanoTime()
        val startEpochMs = PlatformUtils.currentTimeMillis()

        val config = KourierCore.config
        val dataMasker = config.dataMasker
        val truncator = KourierCore.truncator

        // 1. Capture thread call stack (omitting framework and OkHttp frames)
        val callStack: List<CallStackElement> = if (config.captureCallStack) {
            PlatformUtils.captureSanitizedCallStack(config.maxCallStackDepth)
        } else {
            emptyList()
        }

        // 2. Safely read and buffer the request body without exhausting streaming/duplex bodies
        val (requestBodyString, isRequestTruncated, requestBytes) = extractRequestBody(request, truncator.maxPayloadSizeBytes)
        val maskedRequestBody = dataMasker.maskPayload(requestBodyString)

        // 3. Extract & mask request headers
        val requestHeadersList = request.headers.map { it.first to it.second }
        val maskedRequestHeaders = dataMasker.maskHeaders(requestHeadersList)
        val maskedUrl = dataMasker.maskUrl(request.url.toString())

        val httpRequest = HttpRequest(
            url = maskedUrl,
            method = request.method,
            headers = maskedRequestHeaders,
            body = maskedRequestBody,
            contentType = request.body?.contentType()?.toString(),
            contentLength = requestBytes,
            isTruncated = isRequestTruncated,
            uncompressedSizeBytes = requestBytes
        )

        val initialTransaction = HttpTransaction(
            id = transactionId,
            timestamp = startEpochMs,
            request = httpRequest,
            status = TransactionStatus.PENDING,
            timings = HttpTimings(requestStartNs = startNs),
            callStack = callStack,
            totalBytesSent = requestBytes
        )

        KourierCore.recordRequestStarted(initialTransaction)

        val response: Response
        val responseStartNs: Long
        try {
            response = chain.proceed(request)
            responseStartNs = PlatformUtils.nanoTime()
        } catch (e: Exception) {
            val failureNs = PlatformUtils.nanoTime()
            val durationMs = (failureNs - startNs) / 1_000_000L

            val errorPayload = ErrorPayload(
                message = e.message ?: e.toString(),
                exceptionClass = e::class.java.name,
                stackTrace = e.stackTraceToString(),
                timestamp = PlatformUtils.currentTimeMillis(),
                rootCause = e.cause?.toString()
            )

            val failedTransaction = initialTransaction.copy(
                status = TransactionStatus.FAILED,
                error = errorPayload,
                timings = HttpTimings(
                    requestStartNs = startNs,
                    requestEndNs = failureNs,
                    durationMs = durationMs
                )
            )

            KourierCore.recordTransactionCompleted(failedTransaction)
            throw e
        }

        val endNs = PlatformUtils.nanoTime()
        val durationMs = (endNs - startNs) / 1_000_000L
        val ttfbMs = (responseStartNs - startNs) / 1_000_000L

        // 4. Safely inspect response body without consuming downstream stream
        val (responseBodyString, isResponseTruncated, responseBytes) = extractResponseBody(
            response = response,
            maxBytes = truncator.maxPayloadSizeBytes
        )
        val maskedResponseBody = dataMasker.maskPayload(responseBodyString)

        val responseHeadersList = response.headers.map { it.first to it.second }
        val maskedResponseHeaders = dataMasker.maskHeaders(responseHeadersList)

        val httpResponse = HttpResponse(
            statusCode = response.code,
            message = response.message,
            headers = maskedResponseHeaders,
            body = maskedResponseBody,
            contentType = response.body.contentType()?.toString(),
            contentLength = responseBytes,
            isTruncated = isResponseTruncated,
            uncompressedSizeBytes = responseBytes,
            fromCache = response.cacheResponse != null
        )

        val connection = chain.connection()
        val protocol = connection?.protocol()?.toString()
        val remoteAddress = connection?.socket()?.remoteSocketAddress?.toString()

        val completedTransaction = initialTransaction.copy(
            response = httpResponse,
            status = if (response.isSuccessful) TransactionStatus.SUCCESS else TransactionStatus.FAILED,
            timings = HttpTimings(
                requestStartNs = startNs,
                requestEndNs = responseStartNs,
                responseStartNs = responseStartNs,
                responseEndNs = endNs,
                durationMs = durationMs,
                timeToFirstByteMs = ttfbMs
            ),
            protocol = protocol,
            remoteAddress = remoteAddress,
            totalBytesReceived = responseBytes
        )

        KourierCore.recordTransactionCompleted(completedTransaction)
        return response
    }

    private fun extractRequestBody(request: Request, maxBytes: Long): Triple<String?, Boolean, Long> {
        val body = request.body ?: return Triple(null, false, 0L)
        if (body.isOneShot() || body.isDuplex()) {
            return Triple("[One-shot / Duplex stream body not buffered]", false, body.contentLength())
        }

        return try {
            val buffer = Buffer()
            body.writeTo(buffer)

            val originalSize = buffer.size
            val isPlaintext = isPlaintext(buffer)
            if (!isPlaintext) {
                return Triple("[Binary body - $originalSize bytes]", false, originalSize)
            }

            val charset: Charset = body.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            if (originalSize <= maxBytes) {
                Triple(buffer.readString(charset), false, originalSize)
            } else {
                val truncatedText = buffer.readString(maxBytes, charset) +
                        "\n\n/* --- [KOURIER: PAYLOAD TRUNCATED - Capped at ${maxBytes / 1024} KB] --- */"
                Triple(truncatedText, true, originalSize)
            }
        } catch (e: Exception) {
            Triple("[Failed to buffer request body: ${e.message}]", false, 0L)
        }
    }

    private fun extractResponseBody(response: Response, maxBytes: Long): Triple<String?, Boolean, Long> {
        val responseBody = response.body
        if (!response.promisesBody()) {
            return Triple(null, false, 0L)
        }

        val contentType = responseBody.contentType()?.toString()?.lowercase() ?: ""
        val isStreaming = contentType.contains("text/event-stream") ||
                contentType.contains("multipart/x-mixed-replace") ||
                response.code == 101

        if (isStreaming) {
            val length = if (responseBody.contentLength() != -1L) responseBody.contentLength() else 0L
            return Triple("[Streaming Body - Inspection Bypassed]", false, length)
        }

        return try {
            val source = responseBody.source()
            // Request up to maxBytes + extra to see if truncated
            source.request(maxBytes + 1)
            var buffer = source.buffer.clone()

            // Handle gzip transparently if server returned gzipped payload without OkHttp transparent decompression
            val contentEncoding = response.header("Content-Encoding")
            var gzipped = false
            if (contentEncoding.equals("gzip", ignoreCase = true)) {
                gzipped = true
                GzipSource(buffer).use { gzippedSource ->
                    val uncompressedBuffer = Buffer()
                    try {
                        uncompressedBuffer.write(gzippedSource, maxBytes + 1)
                    } catch (_: Exception) {}
                    buffer = uncompressedBuffer
                }
            }

            val totalBytes = if (responseBody.contentLength() != -1L) responseBody.contentLength() else buffer.size
            if (!isPlaintext(buffer)) {
                return Triple("[Binary response - $totalBytes bytes]", false, totalBytes)
            }

            val charset = responseBody.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            if (buffer.size <= maxBytes) {
                val content = buffer.readString(charset)
                Triple(content, false, totalBytes)
            } else {
                val content = buffer.readString(maxBytes, charset) +
                        "\n\n/* --- [KOURIER: RESPONSE TRUNCATED - Capped at ${maxBytes / 1024} KB] --- */"
                Triple(content, true, totalBytes)
            }
        } catch (e: Exception) {
            Triple("[Failed to read response body: ${e.message}]", false, 0L)
        }
    }

    private fun isPlaintext(buffer: Buffer): Boolean {
        return try {
            val prefix = Buffer()
            val byteCount = if (buffer.size < 64) buffer.size else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) break
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            true
        } catch (_: EOFException) {
            false
        }
    }
}
