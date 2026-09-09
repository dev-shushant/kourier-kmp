package dev.shushant.kourier.ui.triggers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.shushant.kourier.core.KourierCore

class KourierNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CLEAR = "dev.shushant.kourier.ui.ACTION_CLEAR"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_CLEAR) {
            KourierCore.clearAll()
        }
    }
}
