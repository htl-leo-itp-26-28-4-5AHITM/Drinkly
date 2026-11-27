import Foundation
import UserNotifications

/// Schedules local drink reminders for the next few days.
/// Rescheduled whenever drinks or settings change, so slots you're already ahead of are skipped.
enum ReminderScheduler {
    private static let prefix = "drinkly-reminder-"
    private static let daysAhead = 3 // iOS keeps at most 64 pending notifications

    static func requestPermission() async {
        _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
    }

    /// `dayStart` is when today began; later days start at the wake-up hour.
    static func reschedule(settings: AppSettings, todayMl: Int, dayStart: Date, now: Date = .now,
                           calendar: Calendar = .current) async {
        let center = UNUserNotificationCenter.current()
        let pending = await center.pendingNotificationRequests().map(\.identifier).filter { $0.hasPrefix(prefix) }
        center.removePendingNotificationRequests(withIdentifiers: pending)

        guard let interval = settings.reminderPlan.intervalHours else { return }
        let today = calendar.startOfDay(for: now)

        for dayOffset in 0..<daysAhead {
            let day = calendar.date(byAdding: .day, value: dayOffset, to: today)!
            let firstHour = dayOffset == 0 ? calendar.component(.hour, from: dayStart) : settings.wakeHour
            for hour in stride(from: firstHour + interval, through: settings.sleepHour - 1, by: interval) {
                guard let slot = calendar.date(bySettingHour: hour, minute: 0, second: 0, of: day), slot > now else { continue }

                if dayOffset == 0 && settings.onlyWhenBehind {
                    let expected = Hydration.expected(
                        by: slot, goalMl: settings.goalMl, start: dayStart, sleepHour: settings.sleepHour
                    )
                    if todayMl >= expected { continue } // already on track for this slot
                }

                let content = UNMutableNotificationContent()
                content.title = "Time for a sip"
                content.body = "Stay on track for your \(Liters.text(settings.goalMl)) goal."
                content.sound = .default

                let parts = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: slot)
                let trigger = UNCalendarNotificationTrigger(dateMatching: parts, repeats: false)
                let id = prefix + ISO8601DateFormatter().string(from: slot)
                try? await center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger))
            }
        }
    }

    static func sendTest(todayMl: Int, goalMl: Int) async {
        await requestPermission()
        let content = UNMutableNotificationContent()
        content.title = "Time for a sip"
        content.body = "You're at \(Liters.value(todayMl)) of \(Liters.text(goalMl)) today."
        content.sound = .default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 2, repeats: false)
        try? await UNUserNotificationCenter.current()
            .add(UNNotificationRequest(identifier: "drinkly-test", content: content, trigger: trigger))
    }
}

/// Shows reminders as banners even while the app is open.
final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationDelegate()

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
