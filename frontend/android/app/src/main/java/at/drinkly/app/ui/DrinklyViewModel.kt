package at.drinkly.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.drinkly.app.bottle.BottleLink
import at.drinkly.app.bottle.BottleState
import at.drinkly.app.bottle.SimulatedBottle
import at.drinkly.app.data.AppSettings
import at.drinkly.app.data.DrinkEntry
import at.drinkly.app.data.DrinklyApi
import at.drinkly.app.data.GoalChange
import at.drinkly.app.data.Hydration
import at.drinkly.app.data.HydrationMessage
import at.drinkly.app.data.HydrationStatus
import at.drinkly.app.data.SettingsStore
import at.drinkly.app.reminders.ReminderScheduler
import at.drinkly.app.reminders.ReminderWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class SyncState(
    val loading: Boolean = false,
    val live: Boolean = false,
    val error: String? = null,
)

/** Everything the screens show, derived from the entries and settings. */
data class Dashboard(
    val settings: AppSettings,
    val entries: List<DrinkEntry>,
    val totals: Map<LocalDate, Int>,
    val today: LocalDate,
    val todayMl: Int,
    val lastHourMl: Int,
    val streak: Int,
    val longestStreak: Int,
    val goalHistory: List<GoalChange>,
    val dayStart: LocalDateTime,
    val message: HydrationMessage,
    val bottle: BottleState,
    val sync: SyncState,
    val now: LocalDateTime,
) {
    val status: HydrationStatus get() = message.status

    /** Share of the goal, may go above 1. */
    val progress: Float get() = todayMl.toFloat() / settings.goalMl

    /** The goal that applied on [day] (the current one for today). */
    fun goalOn(day: LocalDate): Int = Hydration.goalOn(day, goalHistory, settings.goalMl)
}

/** Settings plus the goal history and when today started. */
private data class Profile(val settings: AppSettings, val goalHistory: List<GoalChange>, val firstActive: LocalDateTime?)

class DrinklyViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)
    private val api = DrinklyApi { store.current.backendUrl }
    private val bottle = SimulatedBottle(viewModelScope)

    private val entries = MutableStateFlow<List<DrinkEntry>>(emptyList())
    private val sync = MutableStateFlow(SyncState())
    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000)
        }
    }

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val profile = combine(store.settings, store.goalHistory, store.firstActive, ::Profile)

    val dashboard: StateFlow<Dashboard> =
        combine(entries, profile, bottle.state, sync, clock, ::buildDashboard)
            .stateIn(
                viewModelScope, SharingStarted.Eagerly,
                buildDashboard(
                    emptyList(), Profile(store.current, store.goalHistory.value, store.firstActive.value),
                    bottle.state.value, sync.value, LocalDateTime.now(),
                ),
            )

    init {
        bottle.connect()
        ReminderScheduler.apply(app, store.current)

        // (Re)load and follow the backend's live stream whenever the server or user changes
        viewModelScope.launch {
            store.settings.map { it.backendUrl to it.username }.distinctUntilChanged().collectLatest {
                entries.value = emptyList()
                api.liveEvents().collect { event ->
                    when (event) {
                        DrinklyApi.LiveEvent.Connected -> {
                            sync.update { it.copy(live = true) }
                            load()
                        }
                        DrinklyApi.LiveEvent.Changed -> load()
                        DrinklyApi.LiveEvent.Disconnected -> {
                            sync.update { it.copy(live = false) }
                            load()
                        }
                    }
                }
            }
        }

        // Sips measured by the bottle go straight to the backend
        viewModelScope.launch {
            bottle.sips.collect { addDrink(it, fromBottle = true) }
        }
    }

    fun refresh() {
        store.markActive()
        viewModelScope.launch { load() }
    }

    fun addDrink(milliliters: Int, fromBottle: Boolean = false) {
        viewModelScope.launch {
            try {
                api.add(store.current.username, milliliters)
                load()
                _messages.tryEmit(if (fromBottle) "Bottle synced $milliliters ml" else "Added $milliliters ml")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit("Couldn't save the drink. ${e.message.orEmpty()}".trim())
            }
        }
    }

    fun deleteDrink(entry: DrinkEntry) {
        viewModelScope.launch {
            try {
                api.delete(entry.id)
                load()
                _messages.tryEmit("Removed ${entry.milliliters} ml")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit("Couldn't remove the drink.")
            }
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val before = store.current
        store.update(transform)
        val after = store.current
        if (before.reminderPlan != after.reminderPlan) ReminderScheduler.apply(getApplication(), after)
    }

    fun toggleBottle() {
        if (bottle.state.value.link == BottleLink.DISCONNECTED) bottle.connect() else bottle.disconnect()
    }

    fun simulateSip(milliliters: Int) = bottle.simulateSip(milliliters)

    fun sendTestReminder() {
        val d = dashboard.value
        ReminderWorker.notify(getApplication(), "Time for a sip", "You're at ${d.todayMl} of ${d.settings.goalMl} ml today.")
    }

    private suspend fun load() {
        val settings = store.current
        sync.update { it.copy(loading = true) }
        try {
            entries.value = api.entries(settings.username).sortedByDescending { it.timestamp }
            sync.update { it.copy(error = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sync.update { it.copy(error = "Can't reach the backend at ${settings.backendUrl}") }
        } finally {
            sync.update { it.copy(loading = false) }
        }
    }

    private fun buildDashboard(
        entries: List<DrinkEntry>,
        profile: Profile,
        bottle: BottleState,
        sync: SyncState,
        clockTime: LocalDateTime,
    ): Dashboard {
        // The clock only ticks every minute; new drinks should count right away
        val now = LocalDateTime.now().takeIf { it.isAfter(clockTime) } ?: clockTime
        val settings = profile.settings
        val totals = Hydration.dailyTotals(entries)
        val today = now.toLocalDate()
        val todayMl = totals[today] ?: 0
        val goal = { day: LocalDate -> Hydration.goalOn(day, profile.goalHistory, settings.goalMl) }
        val streak = Hydration.streak(totals, goal, today)
        val lastHourMl = Hydration.lastHour(entries, Instant.now())
        val dayStart = Hydration.dayStart(now, settings.wakeHour, settings.startOnWakeUp, profile.firstActive, entries)
        val expected = Hydration.expectedBy(now.toLocalTime(), settings.goalMl, dayStart.toLocalTime(), settings.sleepHour)
        return Dashboard(
            settings = settings,
            entries = entries,
            totals = totals,
            today = today,
            todayMl = todayMl,
            lastHourMl = lastHourMl,
            streak = streak,
            longestStreak = Hydration.longestStreak(totals, goal),
            goalHistory = profile.goalHistory,
            dayStart = dayStart,
            message = Hydration.message(todayMl, settings.goalMl, expected, lastHourMl, streak),
            bottle = bottle,
            sync = sync,
            now = now,
        )
    }
}
