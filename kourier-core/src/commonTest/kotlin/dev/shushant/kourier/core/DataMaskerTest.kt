package dev.shushant.kourier.core

import dev.shushant.kourier.core.config.DataMasker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataMaskerTest {

    @Test
    fun testPayloadMaskingIsCaseInsensitiveAndRecursive() {
        val payload = """{"PASSWORD":"outer","nested":{"token":"inner"},"safe":"value"}"""
        val masked = DataMasker().maskPayload(payload)!!

        assertTrue("outer" !in masked)
        assertTrue("inner" !in masked)
        assertTrue("value" in masked)
        assertTrue(masked.count { it == '•' } == 16)
    }

    @Test
    fun testHeaderMasking() {
        val masker = DataMasker()
        val rawHeaders = listOf(
            "Authorization" to "Bearer eyJhbGciOi...",
            "Content-Type" to "application/json",
            "Cookie" to "sessionId=123456",
            "Accept" to "*/*"
        )

        val masked = masker.maskHeaders(rawHeaders)
        assertEquals("••••••••", masked.first { it.name == "Authorization" }.value)
        assertTrue(masked.first { it.name == "Authorization" }.isRedacted)

        assertEquals("••••••••", masked.first { it.name == "Cookie" }.value)
        assertTrue(masked.first { it.name == "Cookie" }.isRedacted)

        assertEquals("application/json", masked.first { it.name == "Content-Type" }.value)
    }

    @Test
    fun testUrlMasking() {
        val masker = DataMasker()
        val url = "https://api.example.com/v1/user?apiKey=secret123&sort=asc&token=xyz789"
        val masked = masker.maskUrl(url)

        assertTrue(masked.contains("apiKey=••••••••"))
        assertTrue(masked.contains("token=••••••••"))
        assertTrue(masked.contains("sort=asc"))
    }

    @Test
    fun testPayloadKeyMasking() {
        val masker = DataMasker()
        val jsonPayload = """
            {
                "username": "alice",
                "password": "superSecretPassword123",
                "token": "tok_998877",
                "nested": {
                    "credit_card": "4111111111111111"
                }
            }
        """.trimIndent()

        val masked = masker.maskPayload(jsonPayload) ?: ""
        assertTrue(masked.contains("\"password\": \"••••••••\""))
        assertTrue(masked.contains("\"token\": \"••••••••\""))
        assertTrue(masked.contains("\"credit_card\": \"••••••••\""))
        assertTrue(masked.contains("\"username\": \"alice\""))
    }

    @Test
    fun testPayloadKeyMaskingWithSpecialCharacters() {
        val masker = DataMasker(
            maskedPayloadKeys = setOf("user.password", "auth[token]", "secret(key)", "price$")
        )
        val jsonPayload = """
            {
                "user.password": "pass123",
                "auth[token]": "tok_xyz",
                "secret(key)": 9999,
                "price$": 12.50,
                "normalKey": "visible"
            }
        """.trimIndent()

        val masked = masker.maskPayload(jsonPayload) ?: ""
        assertTrue(masked.contains("\"user.password\": \"••••••••\""))
        assertTrue(masked.contains("\"auth[token]\": \"••••••••\""))
        assertTrue(masked.contains("\"secret(key)\": \"••••••••\""))
        assertTrue(masked.contains("\"price$\": \"••••••••\""))
        assertTrue(masked.contains("\"normalKey\": \"visible\""))
    }

    @Test
    fun testUrlMaskingWithFragment() {
        val masker = DataMasker()
        val url = "https://api.example.com/v1/user?apiKey=secret123&sort=asc#profile"
        val masked = masker.maskUrl(url)

        assertTrue(masked.contains("apiKey=••••••••"))
        assertTrue(masked.contains("sort=asc"))
        assertTrue(masked.endsWith("#profile"))
    }

    @Test
    fun testPayloadObjectAndArrayMasking() {
        val masker = DataMasker(maskedPayloadKeys = setOf("credentials", "tokens"))
        val jsonPayload = """
            {
                "credentials": {
                    "username": "admin",
                    "password": "secret"
                },
                "tokens": ["tok1", "tok2"],
                "status": "ok"
            }
        """.trimIndent()

        val masked = masker.maskPayload(jsonPayload) ?: ""
        assertTrue(masked.contains("\"credentials\": \"••••••••\""))
        assertTrue(masked.contains("\"tokens\": \"••••••••\""))
        assertTrue(masked.contains("\"status\": \"ok\""))
        assertTrue("admin" !in masked)
        assertTrue("secret" !in masked)
        assertTrue("tok1" !in masked)
    }
}

