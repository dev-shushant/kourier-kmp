package dev.shushant.kourier.ui.triggers

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMAccelerometerData
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSDate
import platform.Foundation.NSOperationQueue
import platform.Foundation.timeIntervalSince1970
import kotlin.math.sqrt

@OptIn(ExperimentalForeignApi::class)
actual class ShakeDetector {
    private var motionManager: CMMotionManager? = null
    private var lastShakeTime: Double = 0.0

    actual fun start(onShake: () -> Unit) {
        try {
            val manager = CMMotionManager()
            if (!manager.accelerometerAvailable) return

            manager.accelerometerUpdateInterval = 0.1
            motionManager = manager

            manager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data: CMAccelerometerData?, _ ->
                if (data == null) return@startAccelerometerUpdatesToQueue
                val x = data.acceleration.useContents { x }
                val y = data.acceleration.useContents { y }
                val z = data.acceleration.useContents { z }

                val gForce = sqrt(x * x + y * y + z * z)
                if (gForce > 2.3) {
                    val now = NSDate().timeIntervalSince1970
                    if (now - lastShakeTime > 1.5) {
                        lastShakeTime = now
                        onShake()
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    actual fun stop() {
        try {
            motionManager?.stopAccelerometerUpdates()
        } catch (_: Throwable) {}
        motionManager = null
    }
}
