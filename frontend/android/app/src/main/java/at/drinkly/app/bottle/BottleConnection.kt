package at.drinkly.app.bottle

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

enum class BottleLink { DISCONNECTED, CONNECTING, CONNECTED }

data class BottleState(
    val name: String = "Drinkly Bottle",
    val link: BottleLink = BottleLink.DISCONNECTED,
    val batteryPercent: Int? = null,
    val lastSync: Instant? = null,
)

/**
 * The smart bottle, reached over Bluetooth. The app forwards every sip it reports
 * to the backend, so the bottle itself only ever talks to the phone.
 *
 * [SimulatedBottle] stands in until the bottle's Bluetooth GATT profile is defined;
 * a real implementation only has to provide these members.
 */
interface BottleConnection {
    val state: StateFlow<BottleState>

    /** Amounts in ml the bottle measured since the last sync. */
    val sips: SharedFlow<Int>

    fun connect()
    fun disconnect()
}
