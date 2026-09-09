package dev.shushant.kourier.core

import dev.shushant.kourier.core.engine.CurlGenerator
import dev.shushant.kourier.core.model.HttpHeader
import dev.shushant.kourier.core.model.HttpRequest
import kotlin.test.Test
import kotlin.test.assertTrue

class CurlGeneratorTest {

    @Test
    fun testCurlGeneratorProducesValidCurl() {
        val request = HttpRequest(
            url = "https://api.example.com/v1/users",
            method = "POST",
            headers = listOf(
                HttpHeader("Content-Type", "application/json"),
                HttpHeader("Authorization", "Bearer token123")
            ),
            body = "{\"name\":\"John Doe\"}"
        )

        val curl = CurlGenerator.generate(request)
        assertTrue(curl.startsWith("curl --location --request POST 'https://api.example.com/v1/users'"))
        assertTrue(curl.contains("--header 'Content-Type: application/json'"))
        assertTrue(curl.contains("--header 'Authorization: Bearer token123'"))
        assertTrue(curl.contains("--data-raw '{\"name\":\"John Doe\"}'"))
    }
}
