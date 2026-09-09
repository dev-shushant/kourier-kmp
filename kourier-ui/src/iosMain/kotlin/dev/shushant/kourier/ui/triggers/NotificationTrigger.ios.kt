package dev.shushant.kourier.ui.triggers

import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.core.model.TransactionStatus
import dev.shushant.kourier.ui.KourierUI
import platform.darwin.NSObject
import platform.Foundation.NSLock
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionIcon
import platform.UserNotifications.UNNotificationActionOptionDestructive
import platform.UserNotifications.UNNotificationActionOptionForeground
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionCustomDismissAction
import platform.UserNotifications.UNNotificationDefaultActionIdentifier
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol

private const val ACTION_OPEN = "dev.shushant.kourier.ACTION_OPEN"
private const val ACTION_CLEAR = "dev.shushant.kourier.ACTION_CLEAR"
private const val CATEGORY_TRAFFIC_ID = "dev.shushant.kourier.CATEGORY_TRAFFIC"
private const val CATEGORY_ERROR_ID = "dev.shushant.kourier.CATEGORY_ERROR"
private const val NOTIFICATION_ID = "kourier_live_telemetry"

private class KourierNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        val categoryId = willPresentNotification.request.content.categoryIdentifier
        val options: UNNotificationPresentationOptions = if (categoryId == CATEGORY_ERROR_ID) {
            UNNotificationPresentationOptionBanner or
            UNNotificationPresentationOptionList or
            UNNotificationPresentationOptionSound
        } else {
            UNNotificationPresentationOptionList
        }
        withCompletionHandler(options)
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        try {
            val actionId = didReceiveNotificationResponse.actionIdentifier
            if (actionId == ACTION_OPEN || actionId == UNNotificationDefaultActionIdentifier) {
                try {
                    UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
                } catch (_: Throwable) {}
                KourierUI.show()
            } else if (actionId == ACTION_CLEAR) {
                try {
                    UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium).impactOccurred()
                } catch (_: Throwable) {}
                KourierCore.clearAll()
                NotificationTrigger.clearTransactions()
            }
        } catch (_: Throwable) {}
        withCompletionHandler()
    }
}

actual object NotificationTrigger {
    private var lastErrorCount: Long = 0L
    private var lastErrorTx: HttpTransaction? = null
    private var isAuthorized: Boolean = false
    private val delegate = KourierNotificationDelegate()
    private val recentTransactions = mutableListOf<String>()
    private val lock = NSLock()

    fun recordTransaction(tx: HttpTransaction) {
        val resp = tx.response
        val code = if (resp != null) resp.statusCode else if (tx.status == TransactionStatus.FAILED) 0 else 200
        val indicator = when {
            code in 200..299 -> "🟢"
            code in 300..399 -> "🔵"
            code in 400..499 -> "🟡"
            code >= 500 || code == 0 -> "🔴"
            else -> "⚪"
        }
        val codeLabel = if (code > 0) "$code" else "ERR"
        val path = if (tx.request.path.isNotEmpty()) tx.request.path else tx.request.url
        val durationStr = when {
            tx.durationMs < 1000 -> "${tx.durationMs}ms"
            else -> "${tx.durationMs / 1000}.${(tx.durationMs % 1000) / 100}s"
        }
        val line = "$indicator $codeLabel  ${tx.request.method} $path  ($durationStr)"
        lock.lock()
        try {
            if (code >= 400 || code == 0 || tx.status == TransactionStatus.FAILED) {
                lastErrorTx = tx
            }
            if (recentTransactions.size >= 5) {
                recentTransactions.removeAt(0)
            }
            recentTransactions.add(line)
        } finally {
            lock.unlock()
        }
    }

    fun clearTransactions() {
        lock.lock()
        try {
            recentTransactions.clear()
            lastErrorTx = null
            lastErrorCount = 0L
        } finally {
            lock.unlock()
        }
        try {
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.removeDeliveredNotificationsWithIdentifiers(listOf(NOTIFICATION_ID))
            center.removePendingNotificationRequestsWithIdentifiers(listOf(NOTIFICATION_ID))
        } catch (_: Throwable) {}
    }

    private fun setupCategoriesAndDelegate() {
        try {
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.delegate = delegate

            val openIcon = try {
                UNNotificationActionIcon.iconWithSystemImageName("magnifyingglass")
            } catch (_: Throwable) {
                null
            }
            val clearIcon = try {
                UNNotificationActionIcon.iconWithSystemImageName("trash")
            } catch (_: Throwable) {
                null
            }

            val openAction = if (openIcon != null) {
                UNNotificationAction.actionWithIdentifier(
                    identifier = ACTION_OPEN,
                    title = "Open Inspector",
                    options = UNNotificationActionOptionForeground,
                    icon = openIcon
                )
            } else {
                UNNotificationAction.actionWithIdentifier(
                    identifier = ACTION_OPEN,
                    title = "Open Inspector",
                    options = UNNotificationActionOptionForeground
                )
            }

            val inspectErrorAction = if (openIcon != null) {
                UNNotificationAction.actionWithIdentifier(
                    identifier = ACTION_OPEN,
                    title = "Inspect Error",
                    options = UNNotificationActionOptionForeground,
                    icon = openIcon
                )
            } else {
                UNNotificationAction.actionWithIdentifier(
                    identifier = ACTION_OPEN,
                    title = "Inspect Error",
                    options = UNNotificationActionOptionForeground
                )
            }

            val clearAction = if (clearIcon != null) {
                UNNotificationAction.actionWithIdentifier(
                    identifier = ACTION_CLEAR,
                    title = "Clear Traffic",
                    options = UNNotificationActionOptionDestructive,
                    icon = clearIcon
                )
            } else {
                UNNotificationAction.actionWithIdentifier(
                    identifier = ACTION_CLEAR,
                    title = "Clear Traffic",
                    options = UNNotificationActionOptionDestructive
                )
            }

            val trafficCategory = UNNotificationCategory.categoryWithIdentifier(
                identifier = CATEGORY_TRAFFIC_ID,
                actions = listOf(openAction, clearAction),
                intentIdentifiers = emptyList<String>(),
                options = UNNotificationCategoryOptionCustomDismissAction
            )

            val errorCategory = UNNotificationCategory.categoryWithIdentifier(
                identifier = CATEGORY_ERROR_ID,
                actions = listOf(inspectErrorAction, clearAction),
                intentIdentifiers = emptyList<String>(),
                options = UNNotificationCategoryOptionCustomDismissAction
            )

            center.setNotificationCategories(setOf(trafficCategory, errorCategory))
        } catch (_: Throwable) {}
    }

    private fun requestPermissionIfNeeded() {
        if (isAuthorized) return
        try {
            setupCategoriesAndDelegate()
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { granted, _ ->
                isAuthorized = granted
            }
        } catch (_: Throwable) {}
    }

    actual fun updateNotification(telemetry: TelemetryStats) {
        try {
            requestPermissionIfNeeded()
            if (telemetry.totalRequests == 0L && telemetry.errorCount == 0L) return

            val hasNewErrors = telemetry.errorCount > lastErrorCount
            lastErrorCount = telemetry.errorCount

            val center = UNUserNotificationCenter.currentNotificationCenter()
            val hasErrors = telemetry.errorCount > 0
            val title = if (hasErrors) {
                "⚠️ ${telemetry.errorCount} Errors • ${telemetry.totalRequests} Requests"
            } else {
                "⚡️ Kourier • ${telemetry.totalRequests} Requests Recorded"
            }
            val subtitle = if (telemetry.activeRequests > 0) {
                "⏳ ${telemetry.activeRequests} Active • ${telemetry.formattedThroughput}"
            } else {
                "● Active: ${telemetry.activeRequests} • ${telemetry.formattedThroughput}"
            }

            val body = buildString {
                lock.lock()
                try {
                    if (recentTransactions.isEmpty()) {
                        append("Monitoring HTTP/HTTPS network traffic...\n")
                        append("• Upload: ${TelemetryStats.formatBytes(telemetry.totalBytesSent)}\n")
                        append("• Download: ${TelemetryStats.formatBytes(telemetry.totalBytesReceived)}\n")
                        append("Tap banner or 'Open Inspector' to view details.")
                    } else {
                        for ((idx, line) in recentTransactions.reversed().withIndex()) {
                            if (idx > 0) append("\n")
                            append(line)
                        }
                        append("\n\n📊 Total: ${telemetry.totalRequests} Requests (${telemetry.errorCount} Errors) • ${telemetry.formattedThroughput}")
                    }
                } finally {
                    lock.unlock()
                }
            }

            // Format single notification content — updates in place for both traffic and errors
            val notificationContent = UNMutableNotificationContent().apply {
                if (hasNewErrors) {
                    lock.lock()
                    val errTx = try {
                        lastErrorTx
                    } finally {
                        lock.unlock()
                    }
                    val resp = errTx?.response
                    val errCode = if (resp != null) resp.statusCode else if (errTx?.status == TransactionStatus.FAILED) 0 else 500
                    val codeStr = if (errCode > 0) "$errCode" else "ERR"
                    val path = if (errTx != null && errTx.request.path.isNotEmpty()) errTx.request.path else errTx?.request?.url ?: "Request failed"
                    val method = errTx?.request?.method ?: "HTTP"
                    val durationStr = if (errTx != null) {
                        if (errTx.durationMs < 1000) "${errTx.durationMs}ms"
                        else "${errTx.durationMs / 1000}.${(errTx.durationMs % 1000) / 100}s"
                    } else ""

                    setTitle("🔴 Network Error: $codeStr $method $path")
                    setSubtitle("Request Failed ($durationStr) • ${telemetry.errorCount} Errors")
                    setCategoryIdentifier(CATEGORY_ERROR_ID)
                    setSound(UNNotificationSound.defaultSound())

                    try {
                        UINotificationFeedbackGenerator().apply {
                            prepare()
                            notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
                        }
                    } catch (_: Throwable) {}
                } else {
                    setTitle(title)
                    setSubtitle(subtitle)
                    setCategoryIdentifier(CATEGORY_TRAFFIC_ID)
                }
                setBody(body)
                setThreadIdentifier("kourier_telemetry")
            }

            // Exactly ONE notification is posted to the system, updating in-place
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = NOTIFICATION_ID,
                content = notificationContent,
                trigger = null
            )
            center.addNotificationRequest(request, null)
        } catch (_: Throwable) {}
    }

    actual fun dismiss() {
        try {
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.removeDeliveredNotificationsWithIdentifiers(listOf(NOTIFICATION_ID))
            center.removePendingNotificationRequestsWithIdentifiers(listOf(NOTIFICATION_ID))
            lock.lock()
            try {
                lastErrorCount = 0L
                lastErrorTx = null
                recentTransactions.clear()
            } finally {
                lock.unlock()
            }
        } catch (_: Throwable) {}
    }
}
