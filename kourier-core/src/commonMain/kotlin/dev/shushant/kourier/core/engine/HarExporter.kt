package dev.shushant.kourier.core.engine

import dev.shushant.kourier.core.model.HttpTransaction
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HarRoot(
    val log: HarLog
)

@Serializable
data class HarLog(
    val version: String = "1.2",
    val creator: HarCreator = HarCreator(),
    val entries: List<HarEntry>
)

@Serializable
data class HarCreator(
    val name: String = "Kourier KMP",
    val version: String = "1.0.0"
)

@Serializable
data class HarEntry(
    val startedDateTime: String,
    val time: Long,
    val request: HarRequest,
    val response: HarResponse,
    val cache: HarCache = HarCache(),
    val timings: HarTimings,
    val serverIPAddress: String? = null
)

@Serializable
data class HarNameValuePair(
    val name: String,
    val value: String
)

@Serializable
data class HarRequest(
    val method: String,
    val url: String,
    val httpVersion: String = "HTTP/1.1",
    val headers: List<HarNameValuePair>,
    val queryString: List<HarNameValuePair>,
    val postData: HarPostData? = null,
    val headersSize: Long = -1,
    val bodySize: Long = 0
)

@Serializable
data class HarPostData(
    val mimeType: String,
    val text: String
)

@Serializable
data class HarResponse(
    val status: Int,
    val statusText: String,
    val httpVersion: String = "HTTP/1.1",
    val headers: List<HarNameValuePair>,
    val content: HarContent,
    val redirectURL: String = "",
    val headersSize: Long = -1,
    val bodySize: Long = 0
)

@Serializable
data class HarContent(
    val size: Long,
    val mimeType: String,
    val text: String? = null
)

@Serializable
class HarCache

@Serializable
data class HarTimings(
    val send: Long = 0,
    val wait: Long = 0,
    val receive: Long = 0
)

object HarExporter {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * Converts a list of transactions into a standard HTTP Archive (HAR 1.2) JSON string.
     */
    fun export(transactions: List<HttpTransaction>): String {
        val entries = transactions.map { tx ->
            val isoTime = try {
                Instant.fromEpochMilliseconds(tx.timestamp).toString()
            } catch (_: Exception) {
                "1970-01-01T00:00:00.000Z"
            }

            val queryParams = tx.request.queryParams.map { (k, v) ->
                HarNameValuePair(name = k, value = v)
            }

            val reqHeaders = tx.request.headers.map { HarNameValuePair(it.name, it.value) }
            val postData = tx.request.body?.let {
                HarPostData(
                    mimeType = tx.request.contentType ?: "application/octet-stream",
                    text = it
                )
            }

            val harReq = HarRequest(
                method = tx.request.method,
                url = tx.request.url,
                httpVersion = tx.protocol ?: "HTTP/1.1",
                headers = reqHeaders,
                queryString = queryParams,
                postData = postData,
                bodySize = tx.request.contentLength
            )

            val respHeaders = tx.response?.headers?.map { HarNameValuePair(it.name, it.value) } ?: emptyList()
            val respContent = HarContent(
                size = tx.response?.contentLength ?: 0L,
                mimeType = tx.response?.contentType ?: "application/octet-stream",
                text = tx.response?.body
            )

            val harResp = HarResponse(
                status = tx.response?.statusCode ?: if (tx.error != null) 0 else 200,
                statusText = tx.response?.message ?: if (tx.error != null) "Failed" else "OK",
                httpVersion = tx.protocol ?: "HTTP/1.1",
                headers = respHeaders,
                content = respContent,
                bodySize = tx.response?.contentLength ?: 0L
            )

            val sendTime = tx.timings.requestDurationMs
            val waitTime = tx.timings.timeToFirstByteMs
            val receiveTime = tx.timings.responseDurationMs

            val harTimings = HarTimings(
                send = if (sendTime >= 0) sendTime else 0,
                wait = if (waitTime >= 0) waitTime else 0,
                receive = if (receiveTime >= 0) receiveTime else 0
            )

            HarEntry(
                startedDateTime = isoTime,
                time = tx.durationMs,
                request = harReq,
                response = harResp,
                timings = harTimings,
                serverIPAddress = tx.remoteAddress
            )
        }

        val harRoot = HarRoot(log = HarLog(entries = entries))
        return json.encodeToString(harRoot)
    }

    /** Export transactions as formatted readable plain text. */
    fun exportAsPlainText(transactions: List<HttpTransaction>): String {
        val sb = StringBuilder()
        sb.append("================================================================================\n")
        sb.append("KOURIER KMP NETWORK TELEMETRY EXPORT\n")
        sb.append("Total Transactions: ${transactions.size}\n")
        sb.append("================================================================================\n\n")

        for ((index, tx) in transactions.withIndex()) {
            sb.append("--- [${index + 1}/${transactions.size}] ${tx.request.method} ${tx.request.url} ---\n")
            sb.append("Status: ${tx.formattedStatus} | Duration: ${tx.durationMs}ms | Timestamp: ${tx.timestamp}\n")
            if (tx.protocol != null) sb.append("Protocol: ${tx.protocol}\n")
            sb.append("\n>> REQUEST HEADERS:\n")
            for (h in tx.request.headers) {
                sb.append("  ${h.name}: ${h.value}\n")
            }
            if (!tx.request.body.isNullOrBlank()) {
                sb.append("\n>> REQUEST BODY:\n")
                sb.append(tx.request.body).append("\n")
            }
            if (tx.response != null) {
                sb.append("\n<< RESPONSE HEADERS:\n")
                for (h in tx.response.headers) {
                    sb.append("  ${h.name}: ${h.value}\n")
                }
                if (!tx.response.body.isNullOrBlank()) {
                    sb.append("\n<< RESPONSE BODY:\n")
                    sb.append(tx.response.body).append("\n")
                }
            }
            if (tx.error != null) {
                sb.append("\n!! ERROR:\n")
                sb.append("Exception: ${tx.error.exceptionClass}\n")
                sb.append("Message: ${tx.error.message}\n")
                sb.append("Stack Trace:\n${tx.error.stackTrace}\n")
            }
            sb.append("\n--------------------------------------------------------------------------------\n\n")
        }
        return sb.toString()
    }
}
