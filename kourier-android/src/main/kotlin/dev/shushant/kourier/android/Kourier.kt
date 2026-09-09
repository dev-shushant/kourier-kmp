package dev.shushant.kourier.android

import android.content.Context
import dev.shushant.kourier.core.KourierCore
import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.storage.SQLiteKourierStorage
import dev.shushant.kourier.storage.db.DatabaseDriverFactory
import dev.shushant.kourier.ui.KourierUI
import dev.shushant.kourier.ui.export.AndroidContextHolder

typealias TriggerStyle = dev.shushant.kourier.core.config.TriggerStyle

/**
 * Single entry point for the Kourier SDK on Android.
 *
 * Usage in Application.onCreate():
 *
 *   Kourier.init(this) {
 *       redactHeaders("Authorization")
 *       enableShakeGesture(true)
 *   }
 */
object Kourier {

    /**
     * Initialises Kourier. Call once from Application.onCreate().
     */
    fun init(
        context: Context,
        config: KourierConfig = KourierConfig.Builder().build()
    ) {
        val appContext = context.applicationContext
        AndroidContextHolder.applicationContext = appContext
        val app = (appContext as? android.app.Application) ?: (context as? android.app.Application)
        app?.registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityResumed(activity: android.app.Activity) {
                AndroidContextHolder.currentActivity = activity
            }
            override fun onActivityPaused(activity: android.app.Activity) {
                if (AndroidContextHolder.currentActivity == activity) {
                    AndroidContextHolder.currentActivity = null
                }
            }
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {
                if (AndroidContextHolder.currentActivity == activity) {
                    AndroidContextHolder.currentActivity = null
                }
            }
        })

        val driver = DatabaseDriverFactory(appContext).createDriver()
        val storage = SQLiteKourierStorage(driver)
        KourierCore.initialize(config = config, storage = storage)
        KourierUI.startTriggers()
        app?.let { dev.shushant.kourier.android.ui.AndroidOverlayController.install(it) }
    }

    /** Builder-DSL variant — preferred for multi-option configuration. */
    fun init(
        context: Context,
        block: KourierConfig.Builder.() -> Unit
    ) {
        init(context, KourierConfig.Builder().apply(block).build())
    }

    /** Programmatically opens the Kourier debugger UI. */
    fun showUI() = KourierUI.show()

    /** Programmatically closes the Kourier debugger UI. */
    fun hideUI() = KourierUI.hide()
}
