package dev.shushant.kourier.ui.triggers

expect class ShakeDetector {
    fun start(onShake: () -> Unit)
    fun stop()
}
