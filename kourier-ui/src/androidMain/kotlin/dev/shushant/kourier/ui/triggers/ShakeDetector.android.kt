package dev.shushant.kourier.ui.triggers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dev.shushant.kourier.ui.export.AndroidContextHolder
import kotlin.math.sqrt

actual class ShakeDetector {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var listener: SensorEventListener? = null
    private var lastShakeTime: Long = 0L

    actual fun start(onShake: () -> Unit) {
        val context = AndroidContextHolder.applicationContext ?: return
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val gX = x / SensorManager.GRAVITY_EARTH
                val gY = y / SensorManager.GRAVITY_EARTH
                val gZ = z / SensorManager.GRAVITY_EARTH

                val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

                // Shake threshold ~2.5G
                if (gForce > 2.5f) {
                    val now = System.currentTimeMillis()
                    if (now - lastShakeTime > 1500) { // 1.5s debounce
                        lastShakeTime = now
                        onShake()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    actual fun stop() {
        if (listener != null) {
            sensorManager?.unregisterListener(listener)
            listener = null
        }
    }
}
