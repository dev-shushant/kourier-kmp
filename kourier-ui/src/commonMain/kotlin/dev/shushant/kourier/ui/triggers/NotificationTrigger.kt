package dev.shushant.kourier.ui.triggers

import dev.shushant.kourier.core.model.TelemetryStats

expect object NotificationTrigger {
    fun updateNotification(telemetry: TelemetryStats)
    fun dismiss()
}
