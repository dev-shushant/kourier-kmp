package dev.shushant.kourier.ui.triggers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.core.model.TransactionStatus
import dev.shushant.kourier.ui.activity.KourierActivity
import dev.shushant.kourier.ui.export.AndroidContextHolder
import java.net.URI
import java.util.Collections

actual object NotificationTrigger {
    private const val CHANNEL_TRAY_ID = "kourier_traffic_tray_v4"
    private const val CHANNEL_ALERT_ID = "kourier_error_alerts_v4"
    private const val NOTIFICATION_ID = 404101
    private const val LEGACY_NOTIFICATION_ERROR_ID = 404102

    @Volatile private var channelsCreated = false
    @Volatile private var lastErrorTx: HttpTransaction? = null
    @Volatile private var lastErrorCount = 0L
    private val recentTransactions = Collections.synchronizedList(mutableListOf<String>())

    fun recordTransaction(tx: HttpTransaction) {
        val resp = tx.response
        val code = if (resp != null) resp.statusCode else if (tx.status == TransactionStatus.FAILED) 0 else 200
        if (code >= 400 || code == 0 || tx.status == TransactionStatus.FAILED) {
            lastErrorTx = tx
        }
        val indicator = when {
            code in 200..299 -> "🟢"
            code in 300..399 -> "🔵"
            code in 400..499 -> "🟡"
            code >= 500 || code == 0 -> "🔴"
            else -> "⚪"
        }
        val codeLabel = if (code > 0) "$code" else "ERR"
        val path = if (tx.request.path.isNotEmpty()) {
            tx.request.path
        } else {
            try {
                URI(tx.request.url).path ?: tx.request.url
            } catch (_: Exception) {
                tx.request.url
            }
        }
        val durationStr = when {
            tx.durationMs < 1000 -> "${tx.durationMs}ms"
            else -> "${tx.durationMs / 1000}.${(tx.durationMs % 1000) / 100}s"
        }
        val line = "$indicator $codeLabel  ${tx.request.method} $path  ($durationStr)"
        synchronized(recentTransactions) {
            if (recentTransactions.size >= 5) {
                recentTransactions.removeAt(0)
            }
            recentTransactions.add(line)
        }
    }

    fun clearTransactions() {
        synchronized(recentTransactions) {
            recentTransactions.clear()
        }
        lastErrorTx = null
        lastErrorCount = 0L
        val context = AndroidContextHolder.applicationContext ?: return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(LEGACY_NOTIFICATION_ERROR_ID)
    }

    private fun ensureChannels(notificationManager: NotificationManager) {
        if (channelsCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. Silent ongoing tray channel: docked in drawer, never pops up heads-up on normal requests
            val trayChannel = NotificationChannel(
                CHANNEL_TRAY_ID,
                "Kourier Network Tray",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing network telemetry docked silently in notification drawer"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(trayChannel)

            // 2. High-importance error alert channel: pops up heads-up ONLY when a network error occurs
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Kourier Error Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up alert notifications when network requests fail"
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
        channelsCreated = true
    }

    actual fun updateNotification(telemetry: TelemetryStats) {
        val context = AndroidContextHolder.applicationContext ?: return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        ensureChannels(notificationManager)

        val intent = Intent(context, KourierActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clearIntent = Intent(context, KourierNotificationReceiver::class.java).apply {
            action = KourierNotificationReceiver.ACTION_CLEAR
        }
        val clearPendingIntent = PendingIntent.getBroadcast(
            context,
            11,
            clearIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasNewError = telemetry.errorCount > lastErrorCount
        lastErrorCount = telemetry.errorCount
        val hasErrors = telemetry.errorCount > 0

        val res = context.resources
        val pkg = context.packageName

        val smallIconId = res.getIdentifier("ic_kourier_notification", "drawable", pkg)
        val smallIcon = if (smallIconId != 0) smallIconId else android.R.drawable.stat_notify_sync

        val logoId = res.getIdentifier("kourier_logo_square", "drawable", pkg)
        val largeIconBitmap = if (logoId != 0) {
            try {
                BitmapFactory.decodeResource(res, logoId)
            } catch (_: Exception) {
                null
            }
        } else null

        val inspectIconId = res.getIdentifier("ic_inspect", "drawable", pkg)
        val inspectIcon = if (inspectIconId != 0) inspectIconId else android.R.drawable.ic_menu_search

        val clearIconId = res.getIdentifier("ic_clear", "drawable", pkg)
        val clearIcon = if (clearIconId != 0) clearIconId else android.R.drawable.ic_menu_delete

        val bigText = buildString {
            synchronized(recentTransactions) {
                if (recentTransactions.isEmpty()) {
                    append("Monitoring HTTP/HTTPS network traffic...\n")
                    append("• Upload: ${TelemetryStats.formatBytes(telemetry.totalBytesSent)}\n")
                    append("• Download: ${TelemetryStats.formatBytes(telemetry.totalBytesReceived)}\n")
                    append("Tap notification or 'Open Inspector' below.")
                } else {
                    for ((idx, line) in recentTransactions.reversed().withIndex()) {
                        if (idx > 0) append("\n")
                        append(line)
                    }
                    append("\n\n📊 Total: ${telemetry.totalRequests} Requests (${telemetry.errorCount} Errors) • ${telemetry.formattedThroughput}")
                }
            }
        }

        // Single notification builder — switches channel & styling based on whether a new error occurred
        val channelId = if (hasNewError) CHANNEL_ALERT_ID else CHANNEL_TRAY_ID
        val title = if (hasNewError) {
            val errTx = lastErrorTx
            val errCode = errTx?.response?.statusCode ?: 0
            val codeStr = if (errCode > 0) "$errCode" else "ERR"
            val path = errTx?.request?.path ?: "Request failed"
            val method = errTx?.request?.method ?: "HTTP"
            "🔴 Error $codeStr: $method $path"
        } else if (hasErrors) {
            "⚠️ ${telemetry.errorCount} Errors • ${telemetry.totalRequests} Requests"
        } else {
            "⚡ Kourier • ${telemetry.totalRequests} Requests Recorded"
        }

        val contentText = "${telemetry.formattedThroughput} • ${telemetry.activeRequests} active"
        val color = if (hasErrors) 0xFFDC2626.toInt() else 0xFF00ADB5.toInt()

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(bigText)
            .setBigContentTitle(title)
            .setSummaryText(if (hasErrors) "⚠️ ${telemetry.errorCount} Failed" else "🟢 Active")

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText("Network Monitor")
            .setContentIntent(pendingIntent)
            .setColor(color)
            .setStyle(bigTextStyle)
            .addAction(inspectIcon, if (hasNewError) "Inspect Error" else "Open Inspector", pendingIntent)
            .addAction(clearIcon, "Clear Traffic", clearPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(!hasNewError)
            .setShowWhen(true)
            .setPriority(if (hasNewError) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setCategory(if (hasNewError) NotificationCompat.CATEGORY_ERROR else NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        // Always post to the SINGLE NOTIFICATION_ID, ensuring in-place updates without duplicate entries
        notificationManager.notify(NOTIFICATION_ID, builder.build())
        notificationManager.cancel(LEGACY_NOTIFICATION_ERROR_ID)
    }

    actual fun dismiss() {
        val context = AndroidContextHolder.applicationContext ?: return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(LEGACY_NOTIFICATION_ERROR_ID)
        lastErrorCount = 0L
        lastErrorTx = null
    }
}
