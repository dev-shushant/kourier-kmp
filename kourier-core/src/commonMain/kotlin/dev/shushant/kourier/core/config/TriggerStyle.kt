package dev.shushant.kourier.core.config

/**
 * Defines how Kourier presents live network telemetry and triggers inspection.
 */
enum class TriggerStyle {
    /**
     * Non-intrusive notification tray experience.
     * - Android: System notification in the notification drawer with [Open] and [Clear] actions.
     * - iOS: Ambient notification in Notification Center with [Open Inspector] and [Clear Traffic] actions (zero on-screen clutter).
     */
    NOTIFICATION_TRAY,

    /**
     * Draggable floating bubble on top of the host app screen with live request/error badge.
     */
    FLOATING_BUBBLE,

    /**
     * Both floating bubble and notification tray active simultaneously.
     */
    BOTH,

    /**
     * Pure background inspection: zero visual screen clutter or notifications.
     * Open inspector by shaking the device or calling Kourier.showUI().
     */
    SHAKE_ONLY
}
