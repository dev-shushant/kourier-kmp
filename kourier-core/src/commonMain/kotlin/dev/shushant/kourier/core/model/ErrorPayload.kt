package dev.shushant.kourier.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorPayload(
    val message: String,
    val exceptionClass: String,
    val stackTrace: String,
    val timestamp: Long = 0L,
    val rootCause: String? = null
)
