package dev.shushant.kourier.core.engine

data class TruncationResult(
    val content: String,
    val isTruncated: Boolean,
    val originalSizeBytes: Long,
    val retainedSizeBytes: Long
)

class PayloadTruncator(
    val maxPayloadSizeBytes: Long = DEFAULT_MAX_PAYLOAD_SIZE_BYTES
) {
    companion object {
        const val DEFAULT_MAX_PAYLOAD_SIZE_BYTES: Long = 250 * 1024L // 250 KB
        const val MAX_ALLOWED_PAYLOAD_SIZE_BYTES: Long = 1024 * 1024L // 1 MB
        const val TRUNCATION_NOTICE = "\n\n/* --- [KOURIER: PAYLOAD TRUNCATED TO CAP MEMORY USAGE] --- */"
    }

    /**
     * Safely truncates a byte array if it exceeds the configured byte cap.
     */
    fun truncateBytes(bytes: ByteArray, charsetName: String = "UTF-8"): TruncationResult {
        val originalSize = bytes.size.toLong()
        if (originalSize <= maxPayloadSizeBytes) {
            val content = try {
                bytes.decodeToString()
            } catch (_: Exception) {
                "[Binary Data - ${originalSize} bytes]"
            }
            return TruncationResult(
                content = content,
                isTruncated = false,
                originalSizeBytes = originalSize,
                retainedSizeBytes = originalSize
            )
        }

        val truncatedSlice = bytes.copyOfRange(0, maxPayloadSizeBytes.toInt())
        val decoded = try {
            truncatedSlice.decodeToString() + TRUNCATION_NOTICE
        } catch (_: Exception) {
            "[Binary Data Truncated - original: $originalSize bytes, capped: $maxPayloadSizeBytes bytes]"
        }

        return TruncationResult(
            content = decoded,
            isTruncated = true,
            originalSizeBytes = originalSize,
            retainedSizeBytes = maxPayloadSizeBytes
        )
    }

    /**
     * Safely truncates a string if length or approximate byte size exceeds the cap.
     */
    fun truncateString(text: String?): TruncationResult {
        if (text == null) {
            return TruncationResult(
                content = "",
                isTruncated = false,
                originalSizeBytes = 0L,
                retainedSizeBytes = 0L
            )
        }

        val originalBytes = text.encodeToByteArray()
        return truncateBytes(originalBytes)
    }
}
