package dev.shushant.kourier.core

import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.core.engine.KourierEventBus
import dev.shushant.kourier.core.engine.PayloadTruncator
import dev.shushant.kourier.core.engine.CaptureRuntime
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.storage.KourierStorage
import kotlinx.atomicfu.atomic

object KourierCore {
    private val defaultConfig = KourierConfig()
    private val runtime = atomic<CaptureRuntime?>(null)

    val config: KourierConfig
        get() = runtime.value?.config ?: defaultConfig

    val eventBus: KourierEventBus = KourierEventBus()

    val storage: KourierStorage?
        get() = runtime.value?.storage

    val truncator: PayloadTruncator
        get() = PayloadTruncator(config.maxPayloadSizeBytes)

    fun initialize(
        config: KourierConfig,
        storage: KourierStorage
    ) {
        val replacement = CaptureRuntime(config, storage, eventBus)
        runtime.getAndSet(replacement)?.close()
        eventBus.emitConfigChanged(config)
    }

    /**
     * Dynamically updates the active configuration at runtime (e.g. trigger style, dark mode).
     */
    fun updateConfig(transform: (KourierConfig) -> KourierConfig) {
        val r = runtime.value ?: return
        val updated = transform(r.config)
        r.updateConfig(updated)
        eventBus.emitConfigChanged(updated)
    }

    /**
     * Called by interceptors when a request begins.
     */
    fun recordRequestStarted(transaction: HttpTransaction) {
        runtime.value?.started(transaction)
    }

    /**
     * Called by interceptors when a transaction updates in-flight.
     */
    fun recordTransactionUpdated(transaction: HttpTransaction) {
        runtime.value?.updated(transaction)
    }

    /**
     * Called by interceptors when a response arrives or request fails.
     */
    fun recordTransactionCompleted(transaction: HttpTransaction) {
        runtime.value?.completed(transaction)
    }

    /**
     * Clears all recorded transactions.
     */
    fun clearAll() {
        runtime.value?.clear()
    }

    suspend fun flush() {
        runtime.value?.flush()
    }

    internal fun resetForTests() {
        runtime.getAndSet(null)?.close()
    }
}
