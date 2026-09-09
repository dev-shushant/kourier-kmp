package dev.shushant.kourier.core.config

import dev.shushant.kourier.core.model.HttpHeader

class DataMasker(
    val maskedHeaders: Set<String> = DEFAULT_MASKED_HEADERS,
    val maskedPayloadKeys: Set<String> = DEFAULT_MASKED_KEYS,
    val maskedQueryParams: Set<String> = DEFAULT_MASKED_QUERY_PARAMS,
    val replacementMask: String = "••••••••"
) {
    companion object {
        val DEFAULT_MASKED_HEADERS = setOf(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-auth-token",
            "x-api-key",
            "api-key",
            "session-token",
            "bearer"
        )

        val DEFAULT_MASKED_KEYS = setOf(
            "password",
            "pass",
            "token",
            "access_token",
            "refresh_token",
            "secret",
            "client_secret",
            "api_key",
            "apikey",
            "private_key",
            "ssn",
            "social_security",
            "credit_card",
            "card_number",
            "cvv",
            "pin",
            "auth_code"
        )

        val DEFAULT_MASKED_QUERY_PARAMS = setOf(
            "token",
            "access_token",
            "apiKey",
            "api_key",
            "secret",
            "password"
        )
    }

    private data class CompiledMaskPattern(
        val stringPattern: Regex,
        val primitivePattern: Regex,
        val compositePattern: Regex
    )

    private val precompiledPatterns: List<CompiledMaskPattern> = if (maskedPayloadKeys.isEmpty()) {
        emptyList()
    } else {
        maskedPayloadKeys.map { key ->
            val escaped = Regex.escape(key)
            // Match JSON key with string value, safely handling escaped quotes
            val stringPattern = Regex("""("(?i)$escaped"\s*:\s*")((?:\\.|[^"\\])*)(")""")
            // Match JSON key with number or boolean or null: "key"\s*:\s*([0-9a-zA-Z._-]+)
            val primitivePattern = Regex("""("(?i)$escaped"\s*:\s*)([0-9a-zA-Z._-]+)""")
            // Match JSON key with object or array: "key"\s*:\s*(\{[^{}]*\}|\[[^\[\]]*\])
            val compositePattern = Regex("""("(?i)$escaped"\s*:\s*)(\{[^{}]*\}|\[[^\[\]]*\])""")
            CompiledMaskPattern(stringPattern, primitivePattern, compositePattern)
        }
    }

    /** Mask a list of headers based on configuration. */
    fun maskHeaders(headers: List<Pair<String, String>>): List<HttpHeader> {
        return headers.map { (name, value) ->
            val shouldMask = maskedHeaders.any { it.equals(name.trim(), ignoreCase = true) }
            HttpHeader(
                name = name,
                value = if (shouldMask) replacementMask else value,
                isRedacted = shouldMask
            )
        }
    }

    /** Redact query parameters inside a full URL, preserving any URL fragments. */
    fun maskUrl(url: String): String {
        if (!url.contains("?") || maskedQueryParams.isEmpty()) return url

        val fragment = if (url.contains("#")) "#" + url.substringAfter("#") else ""
        val urlWithoutFragment = url.substringBefore("#")

        val baseUrl = urlWithoutFragment.substringBefore("?")
        val queryString = urlWithoutFragment.substringAfter("?")

        val maskedQuery = queryString.split("&").joinToString("&") { param ->
            val parts = param.split("=", limit = 2)
            val key = parts[0]
            val value = if (parts.size > 1) parts[1] else ""

            val shouldMask = maskedQueryParams.any { it.equals(key.trim(), ignoreCase = true) }
            if (shouldMask) "$key=$replacementMask" else param
        }

        return "$baseUrl?$maskedQuery$fragment"
    }

    /**
     * Redacts JSON payload keys recursively using precompiled regex patterns.
     * Matches patterns like:
     * "password": "value" -> "password": "••••••••"
     * "password": 12345 -> "password": "••••••••"
     * "credentials": { ... } -> "credentials": "••••••••"
     */
    fun maskPayload(payload: String?): String? {
        if (payload.isNullOrBlank() || precompiledPatterns.isEmpty()) return payload

        var currentText: String = payload
        for ((stringPattern, primitivePattern, compositePattern) in precompiledPatterns) {
            currentText = stringPattern.replace(currentText) { matchResult: MatchResult ->
                "${matchResult.groupValues[1]}$replacementMask${matchResult.groupValues[3]}"
            }

            currentText = primitivePattern.replace(currentText) { matchResult: MatchResult ->
                "${matchResult.groupValues[1]}\"$replacementMask\""
            }

            // Loop to handle nested objects or arrays
            var prev: String
            do {
                prev = currentText
                currentText = compositePattern.replace(currentText) { matchResult: MatchResult ->
                    "${matchResult.groupValues[1]}\"$replacementMask\""
                }
            } while (prev != currentText)
        }
        return currentText
    }
}
