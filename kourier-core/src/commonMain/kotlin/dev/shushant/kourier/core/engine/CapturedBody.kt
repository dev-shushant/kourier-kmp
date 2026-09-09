package dev.shushant.kourier.core.engine

data class CapturedBody(
    val text: String? = null,
    val previewBytes: ByteArray? = null,
    val originalSizeBytes: Long? = null,
    val capturedSizeBytes: Long = 0L,
    val truncated: Boolean = false,
    val binary: Boolean = false
)
