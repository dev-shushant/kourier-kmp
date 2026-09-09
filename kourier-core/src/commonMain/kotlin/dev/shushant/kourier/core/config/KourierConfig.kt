package dev.shushant.kourier.core.config

import dev.shushant.kourier.core.engine.PayloadTruncator

data class KourierConfig(
    val maxPayloadSizeBytes: Long = PayloadTruncator.DEFAULT_MAX_PAYLOAD_SIZE_BYTES,
    val maxRetentionCount: Int = 1000,
    val retentionPeriodMs: Long = 3 * 24 * 60 * 60 * 1000L, // 3 days
    val dataMasker: DataMasker = DataMasker(),
    val enableShakeGesture: Boolean = true,
    val enableFloatingBubble: Boolean = true,
    val enableNotification: Boolean = true,
    val captureCallStack: Boolean = true,
    val maxCallStackDepth: Int = 15,
    val darkTheme: Boolean? = null,
    val triggerStyle: TriggerStyle = TriggerStyle.BOTH
) {
    class Builder {
        private var maxPayloadSizeBytes: Long = PayloadTruncator.DEFAULT_MAX_PAYLOAD_SIZE_BYTES
        private var maxRetentionCount: Int = 1000
        private var retentionPeriodMs: Long = 3 * 24 * 60 * 60 * 1000L
        private var maskedHeaders: MutableSet<String> = DataMasker.DEFAULT_MASKED_HEADERS.toMutableSet()
        private var maskedPayloadKeys: MutableSet<String> = DataMasker.DEFAULT_MASKED_KEYS.toMutableSet()
        private var maskedQueryParams: MutableSet<String> = DataMasker.DEFAULT_MASKED_QUERY_PARAMS.toMutableSet()
        private var enableShakeGesture: Boolean = true
        private var enableFloatingBubble: Boolean = true
        private var enableNotification: Boolean = true
        private var captureCallStack: Boolean = true
        private var maxCallStackDepth: Int = 15
        private var darkTheme: Boolean? = null
        private var triggerStyle: TriggerStyle? = null

        fun triggerStyle(style: TriggerStyle) = apply {
            this.triggerStyle = style
            when (style) {
                TriggerStyle.NOTIFICATION_TRAY -> {
                    this.enableNotification = true
                    this.enableFloatingBubble = false
                    this.enableShakeGesture = true
                }
                TriggerStyle.FLOATING_BUBBLE -> {
                    this.enableNotification = false
                    this.enableFloatingBubble = true
                    this.enableShakeGesture = true
                }
                TriggerStyle.BOTH -> {
                    this.enableNotification = true
                    this.enableFloatingBubble = true
                    this.enableShakeGesture = true
                }
                TriggerStyle.SHAKE_ONLY -> {
                    this.enableNotification = false
                    this.enableFloatingBubble = false
                    this.enableShakeGesture = true
                }
            }
        }

        fun maxPayloadSize(bytes: Long) = apply {
            this.maxPayloadSizeBytes = bytes.coerceIn(1024L, PayloadTruncator.MAX_ALLOWED_PAYLOAD_SIZE_BYTES)
        }

        fun maxRetentionCount(count: Int) = apply {
            this.maxRetentionCount = count.coerceAtLeast(10)
        }

        fun retentionPeriodDays(days: Int) = apply {
            this.retentionPeriodMs = days * 24 * 60 * 60 * 1000L
        }

        fun redactHeaders(vararg headers: String) = apply {
            for (h in headers) {
                if (h.isNotBlank()) {
                    this.maskedHeaders.add(h.lowercase().trim())
                }
            }
        }

        fun redactHeaders(headers: List<String>) = apply {
            for (h in headers) {
                if (h.isNotBlank()) {
                    this.maskedHeaders.add(h.lowercase().trim())
                }
            }
        }

        fun redactPayloadKeys(vararg keys: String) = apply {
            for (k in keys) {
                if (k.isNotBlank()) {
                    this.maskedPayloadKeys.add(k.trim())
                }
            }
        }

        fun redactPayloadKeys(keys: List<String>) = apply {
            for (k in keys) {
                if (k.isNotBlank()) {
                    this.maskedPayloadKeys.add(k.trim())
                }
            }
        }

        fun redactQueryParams(vararg params: String) = apply {
            for (p in params) {
                if (p.isNotBlank()) {
                    this.maskedQueryParams.add(p.trim())
                }
            }
        }

        fun redactQueryParams(params: List<String>) = apply {
            for (p in params) {
                if (p.isNotBlank()) {
                    this.maskedQueryParams.add(p.trim())
                }
            }
        }

        fun enableShakeGesture(enable: Boolean) = apply {
            this.enableShakeGesture = enable
        }

        fun enableFloatingBubble(enable: Boolean) = apply {
            this.enableFloatingBubble = enable
        }

        fun enableNotification(enable: Boolean) = apply {
            this.enableNotification = enable
        }

        fun captureCallStack(enable: Boolean, maxDepth: Int = 15) = apply {
            this.captureCallStack = enable
            this.maxCallStackDepth = maxDepth
        }

        fun darkTheme(enable: Boolean?) = apply {
            this.darkTheme = enable
        }

        fun build(): KourierConfig {
            val resolvedStyle = triggerStyle ?: when {
                enableFloatingBubble && enableNotification -> TriggerStyle.BOTH
                enableNotification && !enableFloatingBubble -> TriggerStyle.NOTIFICATION_TRAY
                enableFloatingBubble && !enableNotification -> TriggerStyle.FLOATING_BUBBLE
                else -> TriggerStyle.SHAKE_ONLY
            }
            return KourierConfig(
                maxPayloadSizeBytes = maxPayloadSizeBytes,
                maxRetentionCount = maxRetentionCount,
                retentionPeriodMs = retentionPeriodMs,
                dataMasker = DataMasker(
                    maskedHeaders = maskedHeaders,
                    maskedPayloadKeys = maskedPayloadKeys,
                    maskedQueryParams = maskedQueryParams
                ),
                enableShakeGesture = enableShakeGesture,
                enableFloatingBubble = enableFloatingBubble,
                enableNotification = enableNotification,
                captureCallStack = captureCallStack,
                maxCallStackDepth = maxCallStackDepth,
                darkTheme = darkTheme,
                triggerStyle = resolvedStyle
            )
        }
    }
}
