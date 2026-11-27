package at.drinkly.app.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import at.drinkly.app.data.liters
import at.drinkly.app.data.litersValue
import at.drinkly.app.MainActivity
import at.drinkly.app.R
import at.drinkly.app.data.DrinklyApi
import at.drinkly.app.data.Hydration
import at.drinkly.app.data.HydrationStatus
import at.drinkly.app.data.SettingsStore
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = SettingsStore(applicationContext)
        val settings = store.current
        val now = LocalDateTime.now()
        val entries = runCatching { DrinklyApi { settings.backendUrl }.entries(settings.username) }.getOrNull()
        val dayStart = Hydration.dayStart(now, settings.wakeHour, settings.startOnWakeUp, store.firstActive.value, entries.orEmpty())
        // Not up yet (or already asleep): stay quiet
        if (now.isBefore(dayStart) || now.hour >= settings.sleepHour) return Result.success()

        val todayMl = entries?.let { Hydration.dailyTotals(it)[LocalDate.now()] ?: 0 }

        if (settings.onlyWhenBehind && todayMl != null) {
            val expected = Hydration.expectedBy(now.toLocalTime(), settings.goalMl, dayStart.toLocalTime(), settings.sleepHour)
            val status = Hydration.status(todayMl, settings.goalMl, expected, Hydration.lastHour(entries, Instant.now()))
            if (status != HydrationStatus.BEHIND && status != HydrationStatus.SLIGHTLY_BEHIND) return Result.success()
        }

        val text = if (todayMl != null) "You're at ${litersValue(todayMl)} of ${liters(settings.goalMl)} today." else "Remember to drink some water."
        notify(applicationContext, "Time for a sip", text)
        return Result.success()
    }

    companion object {
        fun notify(context: Context, title: String, text: String) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_drop)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(1, notification)
        }
    }
}
