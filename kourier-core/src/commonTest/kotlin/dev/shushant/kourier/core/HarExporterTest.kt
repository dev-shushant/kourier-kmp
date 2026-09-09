package dev.shushant.kourier.core

import dev.shushant.kourier.core.engine.HarExporter
import dev.shushant.kourier.core.model.HttpHeader
import dev.shushant.kourier.core.model.HttpRequest
import dev.shushant.kourier.core.model.HttpResponse
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TransactionStatus
import kotlin.test.Test
import kotlin.test.assertTrue

class HarExporterTest {

    @Test
    fun testHarExportFormat() {
        val transaction = HttpTransaction(
            id = "test-uuid-1",
            timestamp = 1717000000000L,
            request = HttpRequest(
                url = "https://httpbin.org/get?query=test",
                method = "GET",
                headers = listOf(HttpHeader("User-Agent", "KourierTest"))
            ),
            response = HttpResponse(
                statusCode = 200,
                message = "OK",
                headers = listOf(HttpHeader("Content-Type", "application/json")),
                body = "{\"success\":true}"
            ),
            status = TransactionStatus.SUCCESS
        )

        val harJson = HarExporter.export(listOf(transaction))
        assertTrue(harJson.contains("\"version\": \"1.2\""))
        assertTrue(harJson.contains("\"name\": \"Kourier KMP\""))
        assertTrue(harJson.contains("https://httpbin.org/get?query=test"))
        assertTrue(harJson.contains("\"status\": 200"))
        // HAR is JSON, so an embedded response body must be JSON-escaped.
        assertTrue(harJson.contains("{\\\"success\\\":true}"))

        val plainText = HarExporter.exportAsPlainText(listOf(transaction))
        assertTrue(plainText.contains("KOURIER KMP NETWORK TELEMETRY EXPORT"))
        assertTrue(plainText.contains("GET https://httpbin.org/get?query=test"))
        assertTrue(plainText.contains("200 OK"))
    }
}
