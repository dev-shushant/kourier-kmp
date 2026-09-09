package dev.shushant.kourier.sample.android

import android.app.Application
import dev.shushant.kourier.android.Kourier

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Kourier in Application.onCreate()
        Kourier.init(this) {
            maxPayloadSize(500 * 1024L) // 500 KB limit
            maxRetentionCount(1000) // Keep 1000 recent transactions
            retentionPeriodDays(3) // Auto-purge older than 3 days

            // Mask sensitive headers
            redactHeaders("Authorization", "X-Api-Key", "Cookie", "Set-Cookie")

            // Mask sensitive JSON keys
            redactPayloadKeys("password", "token", "secret", "credit_card", "ssn")

            // Configure trigger style: BOTH (Floating Bubble + Notification Drawer)
            triggerStyle(dev.shushant.kourier.android.TriggerStyle.BOTH)
            captureCallStack(true, maxDepth = 15)
        }
    }
}
