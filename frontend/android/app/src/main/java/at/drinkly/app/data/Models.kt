package at.drinkly.app.data

import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

/** One drink as stored by the backend (`DrinkingRecord`). The backend stores litres. */
data class DrinkEntry(
    val id: Int,
    val timestamp: Instant,
    val liters: Double,
) {
    val milliliters: Int get() = (liters * 1000).roundToInt()
}

enum class ReminderPlan(val title: String, val description: String, val intervalHours: Int?) {
    OFF("Off", "No reminders", null),
    RELAXED("Relaxed", "A nudge every 3 hours", 3),
    BALANCED("Balanced", "A nudge every 2 hours", 2),
    STEADY("Steady", "A nudge every hour", 1),
}

data class AppSettings(
    val username: String = "alice",
    val goalMl: Int = 2000,
    val weightKg: Int = 70,
    // 10.0.2.2 is the host machine as seen from the Android emulator
    val backendUrl: String = "http://10.0.2.2:8080",
    val reminderPlan: ReminderPlan = ReminderPlan.BALANCED,
    val wakeHour: Int = 7,
    val sleepHour: Int = 22,
    val onlyWhenBehind: Boolean = true,
    /** Start the day when you get up (first use of the app or bottle) instead of at [wakeHour]. */
    val startOnWakeUp: Boolean = true,
)

/** The daily goal from [day] on, so past days are judged by the goal that applied back then. */
data class GoalChange(val day: LocalDate, val goalMl: Int)

/** The headline on the Today screen: what's going on and what to do next. */
data class HydrationMessage(val status: HydrationStatus, val title: String, val detail: String)
