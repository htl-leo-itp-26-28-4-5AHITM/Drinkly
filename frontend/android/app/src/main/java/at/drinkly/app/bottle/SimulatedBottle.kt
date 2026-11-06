package at.drinkly.app.bottle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** Demo bottle: pairs after a short delay, slowly drains its battery and sends sips on request. */
class SimulatedBottle(private val scope: CoroutineScope) : BottleConnection {

    private val _state = MutableStateFlow(BottleState())
    private val _sips = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    private var job: Job? = null

    override val state: StateFlow<BottleState> = _state.asStateFlow()
    override val sips: SharedFlow<Int> = _sips.asSharedFlow()

    override fun connect() {
        if (job?.isActive == true) return
        job = scope.launch {
            _state.update { it.copy(link = BottleLink.CONNECTING) }
            delay(1200)
            _state.update {
                it.copy(link = BottleLink.CONNECTED, batteryPercent = it.batteryPercent ?: 85, lastSync = Instant.now())
            }
            while (true) {
                delay(10 * 60 * 1000L)
                _state.update { it.copy(batteryPercent = ((it.batteryPercent ?: 85) - 1).coerceAtLeast(5)) }
            }
        }
    }

    override fun disconnect() {
        job?.cancel()
        _state.update { it.copy(link = BottleLink.DISCONNECTED) }
    }

    fun simulateSip(milliliters: Int) {
        if (_state.value.link != BottleLink.CONNECTED) return
        _state.update { it.copy(lastSync = Instant.now()) }
        _sips.tryEmit(milliliters)
    }
}
