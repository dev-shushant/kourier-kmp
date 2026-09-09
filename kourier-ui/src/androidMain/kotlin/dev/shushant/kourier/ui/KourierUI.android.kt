package dev.shushant.kourier.ui

import android.content.Intent
import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.ui.activity.KourierActivity
import dev.shushant.kourier.ui.export.AndroidContextHolder
import dev.shushant.kourier.ui.triggers.NotificationTrigger
import dev.shushant.kourier.ui.triggers.ShakeDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

actual object KourierUI {
    private var shakeDetector: ShakeDetector? = null
    private var triggersScope: CoroutineScope? = null
    private var notificationJob: Job? = null

    actual fun show() {
        val current = AndroidContextHolder.currentActivity
        if (current is KourierActivity || current?.javaClass?.name?.contains("KourierActivity") == true) {
            return
        }
        val context = current ?: AndroidContextHolder.applicationContext ?: return
        val intent = Intent(context, KourierActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }

    actual fun hide() {
        (AndroidContextHolder.currentActivity as? KourierActivity)?.finish()
    }

    @OptIn(FlowPreview::class)
    actual fun startTriggers() {
        if (triggersScope != null) return

        val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
        triggersScope = scope

        scope.launch {
            KourierCore.eventBus.config.collect { config ->
                // Shake trigger
                if (config.enableShakeGesture) {
                    if (shakeDetector == null) {
                        shakeDetector = ShakeDetector().apply {
                            start { show() }
                        }
                    }
                } else {
                    shakeDetector?.stop()
                    shakeDetector = null
                }

                // Notification trigger
                if (config.enableNotification) {
                    if (notificationJob == null || notificationJob?.isActive == false) {
                        notificationJob = launch(Dispatchers.Default) {
                            launch {
                                KourierCore.eventBus.events.collect { event ->
                                    when (event) {
                                        is dev.shushant.kourier.core.engine.KourierEvent.TransactionCompleted -> {
                                            NotificationTrigger.recordTransaction(event.transaction)
                                        }
                                        is dev.shushant.kourier.core.engine.KourierEvent.TransactionsCleared -> {
                                            NotificationTrigger.clearTransactions()
                                        }
                                        else -> Unit
                                    }
                                }
                            }
                            launch {
                                KourierCore.eventBus.telemetry
                                    .debounce(500L.milliseconds)
                                    .collect { telemetry ->
                                        NotificationTrigger.updateNotification(telemetry)
                                    }
                            }
                        }
                    }
                } else {
                    notificationJob?.cancel()
                    notificationJob = null
                    NotificationTrigger.dismiss()
                }
            }
        }
    }

    actual fun stopTriggers() {
        shakeDetector?.stop()
        shakeDetector = null
        notificationJob?.cancel()
        notificationJob = null
        triggersScope?.cancel()
        triggersScope = null
        NotificationTrigger.dismiss()
    }
}
