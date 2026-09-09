package dev.shushant.kourier.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CallStackElement(
    val className: String,
    val methodName: String,
    val fileName: String? = null,
    val lineNumber: Int = -1,
    val isAppCode: Boolean = false
) {
    override fun toString(): String {
        val location = when {
            fileName != null && lineNumber >= 0 -> "$fileName:$lineNumber"
            fileName != null -> fileName
            else -> "Unknown Source"
        }
        return "$className.$methodName($location)"
    }
}
