package dev.shushant.kourier.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.shushant.kourier.ui.export.AndroidContextHolder
import dev.shushant.kourier.ui.presentation.KourierRootScreen

class KourierActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidContextHolder.currentActivity = this

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        setContent {
            KourierRootScreen(
                onClose = { finish() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidContextHolder.currentActivity = this
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (AndroidContextHolder.currentActivity == this) {
            AndroidContextHolder.currentActivity = null
        }
    }
}
