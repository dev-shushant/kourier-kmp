package dev.shushant.kourier.core.engine

import dev.shushant.kourier.core.config.KourierConfig
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TelemetryStats
import dev.shushant.kourier.core.model.TransactionStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class KourierEvent {
    data class TransactionStarted(val transaction: HttpTransaction) : KourierEvent()
    data class TransactionUpdated(val transaction: HttpTransaction) : KourierEvent()
    data class TransactionCompleted(val transaction: HttpTransaction) : KourierEvent()
    object TransactionsCleared : KourierEvent()
    data class ConfigChanged(val config: KourierConfig) : KourierEvent()
}

class KourierEventBus {
    private val _events = MutableSharedFlow<KourierEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<KourierEvent> = _events.asSharedFlow()

    private val _telemetry = MutableStateFlow(TelemetryStats())
    val telemetry: StateFlow<TelemetryStats> = _telemetry.asStateFlow()

    private val _config = MutableStateFlow(KourierConfig())
    val config: StateFlow<KourierConfig> = _config.asStateFlow()

    fun emitConfigChanged(newConfig: KourierConfig) {
        _config.value = newConfig
        _events.tryEmit(KourierEvent.ConfigChanged(newConfig))
    }

    suspend fun emitStarted(transaction: HttpTransaction) {
        _events.emit(KourierEvent.TransactionStarted(transaction))
        _telemetry.update { current ->
            current.copy(
                totalRequests = current.totalRequests + 1,
                activeRequests = current.activeRequests + 1,
                totalBytesSent = current.totalBytesSent + transaction.totalBytesSent
            )
        }
    }

    suspend fun emitUpdated(transaction: HttpTransaction) {
        _events.emit(KourierEvent.TransactionUpdated(transaction))
    }

    suspend fun emitCompleted(transaction: HttpTransaction) {
        _events.emit(KourierEvent.TransactionCompleted(transaction))
        val isError = transaction.status == TransactionStatus.FAILED ||
                (transaction.response != null && transaction.response.statusCode >= 400)

        _telemetry.update { current ->
            current.copy(
                activeRequests = (current.activeRequests - 1).coerceAtLeast(0),
                errorCount = if (isError) current.errorCount + 1 else current.errorCount,
                totalBytesSent = current.totalBytesSent + (transaction.totalBytesSent - transaction.request.contentLength).coerceAtLeast(0),
                totalBytesReceived = current.totalBytesReceived + transaction.totalBytesReceived
            )
        }
    }

    suspend fun emitCleared() {
        _events.emit(KourierEvent.TransactionsCleared)
        _telemetry.update { current ->
            current.copy(
                totalRequests = 0,
                activeRequests = 0,
                errorCount = 0,
                totalBytesSent = 0,
                totalBytesReceived = 0
            )
        }
    }

    fun setInterceptorActive(active: Boolean) {
        _telemetry.update { it.copy(interceptorActive = active) }
    }
}
