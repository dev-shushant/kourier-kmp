package dev.shushant.kourier.core.config

enum class TriggerStyle {
    NOTIFICATION_TRAY,
    FLOATING_BUBBLE,
    BOTH,
    SHAKE_ONLY
}

class KourierConfig {
    class Builder {
        fun triggerStyle(style: TriggerStyle) = this
        fun maxPayloadSize(bytes: Long) = this
        fun maxRetentionCount(count: Int) = this
        fun retentionPeriodDays(days: Int) = this
        fun redactHeaders(vararg headers: String) = this
        fun redactHeaders(headers: List<String>) = this
        fun redactPayloadKeys(vararg keys: String) = this
        fun redactPayloadKeys(keys: List<String>) = this
        fun redactQueryParams(vararg params: String) = this
        fun redactQueryParams(params: List<String>) = this
        fun enableShakeGesture(enable: Boolean) = this
        fun enableFloatingBubble(enable: Boolean) = this
        fun enableNotification(enable: Boolean) = this
        fun captureCallStack(enable: Boolean, maxDepth: Int = 15) = this
        fun darkTheme(enable: Boolean?) = this
        fun build(): KourierConfig = KourierConfig()
    }
}
