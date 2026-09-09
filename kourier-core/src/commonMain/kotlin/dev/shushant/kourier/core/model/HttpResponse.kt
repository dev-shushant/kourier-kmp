package dev.shushant.kourier.core.model

import kotlinx.serialization.Serializable

@Serializable
data class HttpResponse(
    val statusCode: Int,
    val message: String = "",
    val headers: List<HttpHeader> = emptyList(),
    val body: String? = null,
    val contentType: String? = null,
    val contentLength: Long = 0L,
    val isTruncated: Boolean = false,
    val uncompressedSizeBytes: Long = 0L,
    val fromCache: Boolean = false
) {
    val isSuccessful: Boolean
        get() = statusCode in 200..299

    val isClientError: Boolean
        get() = statusCode in 400..499

    val isServerError: Boolean
        get() = statusCode in 500..599

    fun getHeader(name: String): String? =
        headers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value
}
