package dev.shushant.kourier.android.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dev.shushant.kourier.android.Kourier
import dev.shushant.kourier.android.KourierTelemetry
import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.ui.export.AndroidContextHolder
import dev.shushant.kourier.ui.theme.KourierTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Automatically attaches and detaches the Kourier floating debug bubble to the
 * top resumed Activity, ensuring zero manual layout integration required by host apps.
 */
object AndroidOverlayController : Application.ActivityLifecycleCallbacks {

    private const val OVERLAY_TAG = "kourier_floating_bubble_overlay_tag"
    private var isInstalled = false
    private var currentResumedActivity: WeakReference<Activity>? = null
    private var configScope: CoroutineScope? = null

    fun install(application: Application) {
        if (isInstalled) return
        isInstalled = true
        application.registerActivityLifecycleCallbacks(this)

        configScope = CoroutineScope(Dispatchers.Main.immediate + Job()).apply {
            launch {
                KourierCore.eventBus.config.collect { config ->
                    val activity = currentResumedActivity?.get()
                    if (activity != null && !activity.javaClass.name.contains("KourierActivity")) {
                        if (config.enableFloatingBubble) {
                            attachBubbleToActivity(activity)
                        } else {
                            removeBubbleFromActivity(activity)
                        }
                    }
                }
            }
        }
    }

    fun uninstall(application: Application) {
        if (!isInstalled) return
        isInstalled = false
        configScope?.cancel()
        configScope = null
        application.unregisterActivityLifecycleCallbacks(this)
        currentResumedActivity?.get()?.let { removeBubbleFromActivity(it) }
        currentResumedActivity = null
    }

    override fun onActivityResumed(activity: Activity) {
        currentResumedActivity = WeakReference(activity)
        AndroidContextHolder.currentActivity = activity
        // Skip attaching the bubble inside Kourier's own inspector activity
        if (activity.javaClass.name.contains("KourierActivity")) {
            return
        }
        if (KourierCore.config.enableFloatingBubble) {
            attachBubbleToActivity(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        removeBubbleFromActivity(activity)
        if (currentResumedActivity?.get() == activity) {
            currentResumedActivity = null
        }
        if (AndroidContextHolder.currentActivity == activity) {
            AndroidContextHolder.currentActivity = null
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        removeBubbleFromActivity(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    private fun attachBubbleToActivity(activity: Activity) {
        val rootGroup = activity.findViewById<ViewGroup>(android.R.id.content)
            ?: activity.window?.decorView as? ViewGroup
            ?: return

        // Check if already attached
        if (rootGroup.findViewWithTag<ComposeView>(OVERLAY_TAG) != null) {
            return
        }

        val composeView = ComposeView(activity).apply {
            tag = OVERLAY_TAG
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                KourierTheme {
                    val stats by KourierTelemetry.stats.collectAsState()
                    KourierFloatingBubble(
                        stats = stats,
                        onClick = { Kourier.showUI() }
                    )
                }
            }
        }

        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        rootGroup.addView(composeView, layoutParams)
    }

    private fun removeBubbleFromActivity(activity: Activity) {
        val rootGroup = activity.findViewById<ViewGroup>(android.R.id.content)
            ?: activity.window?.decorView as? ViewGroup
            ?: return

        val existingView = rootGroup.findViewWithTag<ComposeView>(OVERLAY_TAG)
        if (existingView != null) {
            rootGroup.removeView(existingView)
        }
    }
}
