package at.drinkly.app

import android.app.Application
import at.drinkly.app.reminders.ReminderScheduler

class DrinklyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
    }
}
