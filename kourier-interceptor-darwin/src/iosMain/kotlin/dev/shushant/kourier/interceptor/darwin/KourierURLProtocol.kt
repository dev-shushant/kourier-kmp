package dev.shushant.kourier.interceptor.darwin

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.model.ErrorPayload
import dev.shushant.kourier.core.model.HttpRequest
import dev.shushant.kourier.core.model.HttpResponse
import dev.shushant.kourier.core.model.HttpTimings
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TransactionStatus
import dev.shushant.kourier.core.platform.PlatformUtils
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.Foundation.HTTPBody
import platform.Foundation.HTTPBodyStream
import platform.Foundation.HTTPMethod
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSString
import platform.Foundation.NSURLCacheStoragePolicy
import platform.Foundation.NSURLProtocol
import platform.Foundation.NSURLProtocolClientProtocol
import platform.Foundation.NSURLProtocolMeta
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.appendData
import platform.Foundation.create
import platform.Foundation.valueForHTTPHeaderField

private const val KOURIER_HANDLED_KEY = "dev.shushant.kourier.handled"

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KourierURLProtocol : NSURLProtocol, NSURLSessionDataDelegateProtocol {

    private var activeTask: NSURLSessionDataTask? = null
    private var internalSession: NSURLSession? = null
    private var transactionId: String = ""
    private var startNs: Long = 0L
    private var startEpochMs: Long = 0L
    private var responseStartNs: Long = 0L
    private var totalBytesReceived: Long = 0L
    private var receivedData: NSMutableData? = null
    private var httpResponse: NSHTTPURLResponse? = null
    private var initialTx: HttpTransaction? = null

    // NSURLProtocol NS_DESIGNATED_INITIALIZER — CFNetwork instantiates the
    // protocol via -initWithRequest:cachedResponse:client:. Kotlin/Native
    // requires @OverrideInit so it registers this constructor as the ObjC
    // designated initializer and stops throwing 'Initializer is not implemented'.
    @OverrideInit
    constructor(
        request: NSURLRequest,
        cachedResponse: platform.Foundation.NSCachedURLResponse?,
        client: NSURLProtocolClientProtocol?
    ) : super(request, cachedResponse, client)

    companion object : NSURLProtocolMeta() {
        override fun canInitWithRequest(request: NSURLRequest): Boolean {
            val isHandled = propertyForKey(KOURIER_HANDLED_KEY, inRequest = request) != null
            val scheme = request.URL?.scheme?.lowercase()
            val isHttp = scheme == "http" || scheme == "https"
            return isHttp && !isHandled
        }

        override fun canonicalRequestForRequest(request: NSURLRequest): NSURLRequest {
            return request
        }
    }

    override fun startLoading() {
        val rawRequest = request
        val mutableRequest = rawRequest.mutableCopy() as NSMutableURLRequest
        Companion.setProperty("YES", forKey = KOURIER_HANDLED_KEY, inRequest = mutableRequest)

        transactionId = PlatformUtils.randomUuid()
        startNs = PlatformUtils.nanoTime()
        startEpochMs = PlatformUtils.currentTimeMillis()
        totalBytesReceived = 0L
        receivedData = NSMutableData()

        val config = KourierCore.config
        val dataMasker = config.dataMasker
        val truncator = KourierCore.truncator

        val callStack = if (config.captureCallStack) {
            PlatformUtils.captureSanitizedCallStack(config.maxCallStackDepth)
        } else {
            emptyList()
        }

        // Extract and mask request headers
        val rawHeaders = mutableListOf<Pair<String, String>>()
        rawRequest.allHTTPHeaderFields?.forEach { (k, v) ->
            val key = k?.toString()
            val value = v?.toString()
            if (key != null && value != null) {
                rawHeaders.add(key to value)
            }
        }
        val maskedHeaders = dataMasker.maskHeaders(rawHeaders)

        // Read request body safely
        val httpBody = rawRequest.HTTPBody
        val bodyBytes = httpBody?.length?.toLong() ?: 0L
        var requestBodyStr: String? = null
        var isTruncated = false
        if (httpBody != null && bodyBytes > 0) {
            val bodyString = NSString.create(data = httpBody, encoding = NSUTF8StringEncoding)?.toString()
            if (bodyString != null) {
                val truncResult = truncator.truncateString(bodyString)
                requestBodyStr = dataMasker.maskPayload(truncResult.content)
                isTruncated = truncResult.isTruncated
            }
        } else if (rawRequest.HTTPBodyStream != null) {
            requestBodyStr = "[Stream request body - not buffered]"
        }

        val urlString = rawRequest.URL?.absoluteString ?: ""
        val maskedUrl = dataMasker.maskUrl(urlString)
        val method = rawRequest.HTTPMethod ?: "GET"
        val contentType = rawRequest.valueForHTTPHeaderField("Content-Type")

        val httpRequest = HttpRequest(
            url = maskedUrl,
            method = method,
            headers = maskedHeaders,
            body = requestBodyStr,
            contentType = contentType,
            contentLength = bodyBytes,
            isTruncated = isTruncated,
            uncompressedSizeBytes = bodyBytes
        )

        initialTx = HttpTransaction(
            id = transactionId,
            timestamp = startEpochMs,
            request = httpRequest,
            status = TransactionStatus.PENDING,
            timings = HttpTimings(requestStartNs = startNs),
            callStack = callStack,
            totalBytesSent = bodyBytes
        )

        initialTx?.let { KourierCore.recordRequestStarted(it) }

        // Use ephemeralSessionConfiguration with an EMPTY protocolClasses list so
        // KourierURLProtocol is NOT re-applied to the forwarded request.
        // defaultSessionConfiguration inherits all registered protocols, causing
        // infinite interception even with the KOURIER_HANDLED_KEY marker.
        val sessionConfig = NSURLSessionConfiguration.ephemeralSessionConfiguration
        sessionConfig.protocolClasses = emptyList<Any>()
        val backgroundQueue = NSOperationQueue().apply {
            maxConcurrentOperationCount = 4
        }
        internalSession = NSURLSession.sessionWithConfiguration(
            configuration = sessionConfig,
            delegate = this,
            delegateQueue = backgroundQueue
        )
        activeTask = internalSession?.dataTaskWithRequest(mutableRequest)
        activeTask?.resume()
    }

    override fun stopLoading() {
        activeTask?.cancel()
        activeTask = null
        internalSession?.invalidateAndCancel()
        internalSession = null
        receivedData = null
        httpResponse = null
        initialTx = null
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        willPerformHTTPRedirection: NSHTTPURLResponse,
        newRequest: NSURLRequest,
        completionHandler: (NSURLRequest?) -> Unit
    ) {
        val mutableNewReq = (newRequest.mutableCopy() as? NSMutableURLRequest)
            ?: NSMutableURLRequest.requestWithURL(newRequest.URL!!)
        Companion.setProperty("YES", forKey = KOURIER_HANDLED_KEY, inRequest = mutableNewReq)
        client?.URLProtocol(this, wasRedirectedToRequest = mutableNewReq, redirectResponse = willPerformHTTPRedirection)
        completionHandler(mutableNewReq)
    }

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveResponse: NSURLResponse,
        completionHandler: (platform.Foundation.NSURLSessionResponseDisposition) -> Unit
    ) {
        responseStartNs = PlatformUtils.nanoTime()
        httpResponse = didReceiveResponse as? NSHTTPURLResponse
        // NSURLCacheStorageNotAllowed = 2UL
        client?.URLProtocol(
            this,
            didReceiveResponse = didReceiveResponse,
            cacheStoragePolicy = NSURLCacheStoragePolicy.NSURLCacheStorageNotAllowed
        )
        completionHandler(platform.Foundation.NSURLSessionResponseAllow)
    }

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveData: NSData
    ) {
        val chunkLen = didReceiveData.length.toLong()
        totalBytesReceived += chunkLen

        val maxBytes = KourierCore.truncator.maxPayloadSizeBytes
        val currentBuffered = receivedData?.length?.toLong() ?: 0L
        if (currentBuffered < maxBytes) {
            val remainingToCap = maxBytes - currentBuffered
            if (chunkLen <= remainingToCap) {
                receivedData?.appendData(didReceiveData)
            } else {
                val subdata = NSData.create(bytes = didReceiveData.bytes, length = remainingToCap.toULong())
                receivedData?.appendData(subdata)
            }
        }
        client?.URLProtocol(this, didLoadData = didReceiveData)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?
    ) {
        val endNs = PlatformUtils.nanoTime()
        val durationMs = (endNs - startNs) / 1_000_000L
        val ttfbMs = if (responseStartNs > startNs) (responseStartNs - startNs) / 1_000_000L else 0L

        val config = KourierCore.config
        val dataMasker = config.dataMasker
        val truncator = KourierCore.truncator

        if (didCompleteWithError != null) {
            val errorPayload = ErrorPayload(
                message = didCompleteWithError.localizedDescription,
                exceptionClass = didCompleteWithError.domain ?: "NSError",
                stackTrace = didCompleteWithError.userInfo.toString(),
                timestamp = PlatformUtils.currentTimeMillis()
            )

            val failedTx = initialTx?.copy(
                status = TransactionStatus.FAILED,
                error = errorPayload,
                timings = HttpTimings(
                    requestStartNs = startNs,
                    requestEndNs = endNs,
                    durationMs = durationMs
                )
            )
            failedTx?.let { KourierCore.recordTransactionCompleted(it) }

            // Notify client after recording
            client?.URLProtocol(this, didFailWithError = didCompleteWithError)
        } else {
            val resp = httpResponse
            val statusCode = resp?.statusCode?.toInt() ?: 200

            val rawHeaders = mutableListOf<Pair<String, String>>()
            resp?.allHeaderFields?.forEach { (k, v) ->
                val key = k?.toString()
                val value = v?.toString()
                if (key != null && value != null) {
                    rawHeaders.add(key to value)
                }
            }
            val maskedRespHeaders = dataMasker.maskHeaders(rawHeaders)

            var responseBodyStr: String? = null
            val bufferedLen = receivedData?.length?.toLong() ?: 0L
            var isTruncated = totalBytesReceived > bufferedLen

            if (receivedData != null && bufferedLen > 0) {
                val str = NSString.create(
                    data = receivedData!!,
                    encoding = NSUTF8StringEncoding
                )?.toString()
                if (str != null) {
                    val truncResult = truncator.truncateString(str)
                    responseBodyStr = dataMasker.maskPayload(truncResult.content)
                    isTruncated = isTruncated || truncResult.isTruncated
                } else {
                    responseBodyStr = "[Binary response - $totalBytesReceived bytes]"
                }
            }

            val contentType = resp?.allHeaderFields?.get("Content-Type")?.toString()

            val domainResponse = HttpResponse(
                statusCode = statusCode,
                message = if (statusCode in 200..299) "OK" else "Error",
                headers = maskedRespHeaders,
                body = responseBodyStr,
                contentType = contentType,
                contentLength = totalBytesReceived,
                isTruncated = isTruncated,
                uncompressedSizeBytes = totalBytesReceived
            )

            val completedTx = initialTx?.copy(
                response = domainResponse,
                status = if (statusCode in 200..299) TransactionStatus.SUCCESS else TransactionStatus.FAILED,
                timings = HttpTimings(
                    requestStartNs = startNs,
                    requestEndNs = responseStartNs,
                    responseStartNs = responseStartNs,
                    responseEndNs = endNs,
                    durationMs = durationMs,
                    timeToFirstByteMs = ttfbMs
                ),
                totalBytesReceived = totalBytesReceived
            )
            completedTx?.let { KourierCore.recordTransactionCompleted(it) }

            // Notify client after recording
            client?.URLProtocolDidFinishLoading(this)
        }

        // Clean up references
        activeTask = null
        internalSession?.invalidateAndCancel()
        internalSession = null
        receivedData = null
        httpResponse = null
        initialTx = null
    }
}
