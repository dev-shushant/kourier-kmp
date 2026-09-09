package dev.shushant.kourier.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED
}

@Serializable
data class HttpTransaction(
    val id: String,
    val timestamp: Long,
    val request: HttpRequest,
    val response: HttpResponse? = null,
    val error: ErrorPayload? = null,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val timings: HttpTimings = HttpTimings(),
    val callStack: List<CallStackElement> = emptyList(),
    val protocol: String? = null,
    val tlsVersion: String? = null,
    val localAddress: String? = null,
    val remoteAddress: String? = null,
    val totalBytesSent: Long = 0L,
    val totalBytesReceived: Long = 0L,
    val tags: Map<String, String> = emptyMap()
) {
    val durationMs: Long
        get() = timings.calculatedTotalMs

    val isPending: Boolean
        get() = status == TransactionStatus.PENDING

    val isFailed: Boolean
        get() = status == TransactionStatus.FAILED || (response != null && response.statusCode >= 400)

    val statusCode: Int?
        get() = response?.statusCode

    val formattedStatus: String
        get() = when {
            status == TransactionStatus.PENDING -> "PENDING"
            error != null -> "FAILED"
            response != null -> "${response.statusCode} ${response.message}".trim()
            else -> "UNKNOWN"
        }
}
