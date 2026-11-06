package at.drinkly.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

/** Profile and reminder settings, persisted in SharedPreferences. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("drinkly", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())

    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    val current: AppSettings get() = _settings.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
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
        }
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
        )
    }
}
