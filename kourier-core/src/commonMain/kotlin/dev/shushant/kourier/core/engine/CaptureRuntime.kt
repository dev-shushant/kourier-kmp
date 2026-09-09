package dev.shushant.kourier.core.engine

import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.platform.PlatformUtils
import dev.shushant.kourier.core.storage.KourierStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

internal class CaptureRuntime(
    @kotlin.concurrent.Volatile var config: KourierConfig,
    val storage: KourierStorage,
    private val eventBus: KourierEventBus
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val events = Channel<Event>(capacity = 1000, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var requestCounter = 0

    fun updateConfig(newConfig: KourierConfig) {
        this.config = newConfig
    }

    init {
        scope.launch {
            for (event in events) process(event)
        }
        events.trySend(Event.Maintain)
    }

    fun started(transaction: HttpTransaction) {
        events.trySend(Event.Started(transaction))
    }

    fun updated(transaction: HttpTransaction) {
        events.trySend(Event.Updated(transaction))
    }

    fun completed(transaction: HttpTransaction) {
        events.trySend(Event.Completed(transaction))
    }

    fun clear() {
        events.trySend(Event.Clear)
    }

    suspend fun flush() {
        val barrier = CompletableDeferred<Unit>()
        if (events.trySend(Event.Barrier(barrier)).isSuccess) barrier.await()
    }

    fun close() {
        events.close()
        scope.cancel()
    }

    private suspend fun process(event: Event) {
        when (event) {
            Event.Maintain -> {
                persist { storage.trimToCount(config.maxRetentionCount) }
                val cutoff = PlatformUtils.currentTimeMillis() - config.retentionPeriodMs
                persist { storage.deleteOlderThan(cutoff) }
            }
            is Event.Started -> {
                persist { storage.insertOrUpdate(event.transaction) }
                eventBus.emitStarted(event.transaction)
                if (++requestCounter % 50 == 0) {
                    persist { storage.trimToCount(config.maxRetentionCount) }
                }
            }
            is Event.Updated -> {
                persist { storage.insertOrUpdate(event.transaction) }
                eventBus.emitUpdated(event.transaction)
            }
            is Event.Completed -> {
                persist { storage.insertOrUpdate(event.transaction) }
                eventBus.emitCompleted(event.transaction)
            }
            Event.Clear -> {
                persist { storage.clearAll() }
                eventBus.emitCleared()
            }
            is Event.Barrier -> event.completion.complete(Unit)
        }
    }

    private suspend fun persist(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: Throwable) {
            // Inspection is fail-open: persistence must never affect host traffic.
        }
    }

    private sealed interface Event {
        data object Maintain : Event
        data class Started(val transaction: HttpTransaction) : Event
        data class Updated(val transaction: HttpTransaction) : Event
        data class Completed(val transaction: HttpTransaction) : Event
        data object Clear : Event
        data class Barrier(val completion: CompletableDeferred<Unit>) : Event
    }
}
