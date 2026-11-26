package at.drinkly.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit
import java.time.LocalDate
import java.time.LocalDateTime

/** Profile and reminder settings, the goal history and when today started, persisted in SharedPreferences. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("drinkly", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    private val _goalHistory = MutableStateFlow(loadGoalHistory())
    private val _firstActive = MutableStateFlow(prefs.getString("firstActive", null)?.let(LocalDateTime::parse))

    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    val current: AppSettings get() = _settings.value

    /** Goal changes over time, oldest first. */
    val goalHistory: StateFlow<List<GoalChange>> = _goalHistory.asStateFlow()

    /** The first time the app was opened today, used to detect when the day started. */
    val firstActive: StateFlow<LocalDateTime?> = _firstActive.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val before = _settings.value
        val next = transform(before)
        _settings.value = next
        prefs.edit {
            putString("username", next.username)
            putInt("goalMl", next.goalMl)
            putInt("weightKg", next.weightKg)
            putString("backendUrl", next.backendUrl)
            putString("reminderPlan", next.reminderPlan.name)
            putInt("wakeHour", next.wakeHour)
            putInt("sleepHour", next.sleepHour)
            putBoolean("onlyWhenBehind", next.onlyWhenBehind)
            putBoolean("startOnWakeUp", next.startOnWakeUp)
        }
        if (before.goalMl != next.goalMl) {
            _goalHistory.value = Hydration.recordGoal(next.goalMl, LocalDate.now(), _goalHistory.value)
            prefs.edit { putString("goalHistory", _goalHistory.value.joinToString(";") { "${it.day}=${it.goalMl}" }) }
        }
    }

    /** Remembers the first time the app is used each day, as a sign that you got up. */
    fun markActive(now: LocalDateTime = LocalDateTime.now()) {
        val first = _firstActive.value
        if (first != null && first.toLocalDate() == now.toLocalDate()) return
        if (now.hour < Hydration.EARLIEST_DAY_START_HOUR) return
        _firstActive.value = now
        prefs.edit { putString("firstActive", now.toString()) }
    }

    private fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            username = prefs.getString("username", null) ?: d.username,
            goalMl = prefs.getInt("goalMl", d.goalMl),
            weightKg = prefs.getInt("weightKg", d.weightKg),
            backendUrl = prefs.getString("backendUrl", null) ?: d.backendUrl,
            reminderPlan = prefs.getString("reminderPlan", null)
                ?.let { name -> ReminderPlan.entries.firstOrNull { it.name == name } }
                ?: d.reminderPlan,
            wakeHour = prefs.getInt("wakeHour", d.wakeHour),
            sleepHour = prefs.getInt("sleepHour", d.sleepHour),
            onlyWhenBehind = prefs.getBoolean("onlyWhenBehind", d.onlyWhenBehind),
            startOnWakeUp = prefs.getBoolean("startOnWakeUp", d.startOnWakeUp),
        )
    }

    /** Stored as "2026-11-01=2000;2026-11-12=2500"; starts with today's goal when nothing was saved yet. */
    private fun loadGoalHistory(): List<GoalChange> {
        val saved = prefs.getString("goalHistory", null).orEmpty().split(';').mapNotNull { part ->
            val (day, goal) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            runCatching { GoalChange(LocalDate.parse(day), goal.toInt()) }.getOrNull()
        }
        return saved.ifEmpty { listOf(GoalChange(LocalDate.now(), _settings.value.goalMl)) }
    }
}
