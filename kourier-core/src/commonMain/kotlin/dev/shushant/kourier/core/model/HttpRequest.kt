package dev.shushant.kourier.core.model

import kotlinx.serialization.Serializable

@Serializable
data class HttpHeader(
    val name: String,
    val value: String,
    val isRedacted: Boolean = false
)

@Serializable
data class HttpRequest(
    val url: String,
    val method: String,
    val headers: List<HttpHeader> = emptyList(),
    val body: String? = null,
    val contentType: String? = null,
    val contentLength: Long = 0L,
    val isTruncated: Boolean = false,
    val uncompressedSizeBytes: Long = 0L
) {
    val isSsl: Boolean
        get() = url.startsWith("https://", ignoreCase = true)

    val host: String
        get() = try {
            val withoutScheme = if (url.contains("://")) url.substringAfter("://") else url
            withoutScheme.substringBefore("/").substringBefore("?")
        } catch (_: Exception) {
            "unknown"
        }

    val path: String
        get() = try {
            val withoutScheme = if (url.contains("://")) url.substringAfter("://") else url
            val pathPart = withoutScheme.substringAfter("/", "")
            if (pathPart.isNotEmpty()) "/${pathPart.substringBefore("?")}" else "/"
        } catch (_: Exception) {
            "/"
        }

    val queryParams: Map<String, String>
        get() = try {
            if (!url.contains("?")) emptyMap()
            else {
                url.substringAfter("?")
                    .split("&")
                    .filter { it.isNotEmpty() }
                    .associate {
                        val parts = it.split("=", limit = 2)
                        val k = parts[0]
                        val v = if (parts.size > 1) parts[1] else ""
                        k to v
                    }
            }
        } catch (_: Exception) {
            emptyMap()
        }

    fun getHeader(name: String): String? =
        headers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value
}
