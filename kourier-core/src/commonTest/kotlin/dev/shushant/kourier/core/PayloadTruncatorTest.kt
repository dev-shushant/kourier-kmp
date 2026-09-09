package dev.shushant.kourier.core

import dev.shushant.kourier.core.engine.PayloadTruncator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PayloadTruncatorTest {

    @Test
    fun testPayloadWithinLimitNotTruncated() {
        val truncator = PayloadTruncator(maxPayloadSizeBytes = 1000)
        val text = "Hello Kourier!"
        val result = truncator.truncateString(text)

        assertFalse(result.isTruncated)
        assertEquals(text, result.content)
        assertEquals(text.encodeToByteArray().size.toLong(), result.originalSizeBytes)
    }

    @Test
    fun testPayloadExceedingLimitTruncated() {
        val limit = 50L
        val truncator = PayloadTruncator(maxPayloadSizeBytes = limit)
        val largeText = "A".repeat(200)
        val result = truncator.truncateString(largeText)

        assertTrue(result.isTruncated)
        assertEquals(200L, result.originalSizeBytes)
        assertEquals(limit, result.retainedSizeBytes)
        assertTrue(result.content.contains("PAYLOAD TRUNCATED"))
    }
}
