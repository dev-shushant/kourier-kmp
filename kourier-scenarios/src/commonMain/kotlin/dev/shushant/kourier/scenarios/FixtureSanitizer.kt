package dev.shushant.kourier.scenarios

import kotlinx.serialization.json.*

data class SanitizedFixtureBody(val body: String, val redactedFieldCount: Int)

/** Sanitize parsed JSON before fixture preview/persistence, including whole sensitive composites. */
object FixtureSanitizer {
    val DEFAULT_SENSITIVE_KEYS = setOf(
        "password", "pass", "token", "access_token", "refresh_token", "secret", "client_secret",
        "api_key", "apikey", "private_key", "ssn", "social_security", "credit_card", "card_number",
        "cvv", "pin", "auth_code"
    )

    fun sanitizeJson(body: String, sensitiveKeys: Set<String> = DEFAULT_SENSITIVE_KEYS, mask: String = "••••••••"): SanitizedFixtureBody? {
        val parsed = BoundedJson.parse(body, ScenarioValidation.MAX_FIXTURE_BYTES) ?: return null
        val keys = (DEFAULT_SENSITIVE_KEYS + sensitiveKeys).map { it.lowercase() }.toSet()
        var count = 0
        fun sanitize(element: JsonElement): JsonElement = when (element) {
            is JsonObject -> JsonObject(element.mapValues { (key, value) ->
                if (key.lowercase() in keys) { count++; JsonPrimitive(mask) } else sanitize(value)
            })
            is JsonArray -> JsonArray(element.map { sanitize(it) })
            else -> element
        }
        val result = sanitize(parsed).toString()
        if (result.encodeToByteArray().size > ScenarioValidation.MAX_FIXTURE_BYTES) return null
        return SanitizedFixtureBody(result, count)
    }
}
