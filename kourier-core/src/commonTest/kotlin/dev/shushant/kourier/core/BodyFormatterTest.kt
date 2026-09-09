package dev.shushant.kourier.core

import dev.shushant.kourier.core.engine.BodyFormat
import dev.shushant.kourier.core.engine.BodyFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BodyFormatterTest {
    @Test
    fun formatsJsonAndPreservesRawValue() {
        val raw = "{\"name\":\"Kourier\",\"enabled\":true}"
        val result = BodyFormatter.format("application/json", raw)

        assertEquals(BodyFormat.JSON, result.format)
        assertEquals(raw, result.raw)
        assertTrue(result.formatted.contains("\n"))
        assertTrue(result.formatted.contains("\"enabled\": true"))
    }

    @Test
    fun formatsWellFormedXml() {
        val result = BodyFormatter.format("application/xml", "<root><item>one</item></root>")

        assertEquals(BodyFormat.XML, result.format)
        assertEquals("<root>\n  <item>one</item>\n</root>", result.formatted)
    }

    @Test
    fun malformedStructuredContentFallsBackToRawText() {
        val raw = "{not-json"
        val result = BodyFormatter.format("application/json", raw)

        assertEquals(BodyFormat.TEXT, result.format)
        assertEquals(raw, result.formatted)
    }
}
