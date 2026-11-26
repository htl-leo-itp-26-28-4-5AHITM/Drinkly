import Foundation
import SwiftUI

/// App state shared by all screens: settings, drinks from the backend and the bottle.
@MainActor
final class AppModel: ObservableObject {

    @Published var settings: AppSettings {
        didSet { settingsChanged(from: oldValue) }
    }
    @Published private(set) var entries: [DrinkEntry] = []
    @Published private(set) var isLive = false
    @Published private(set) var errorMessage: String?
    @Published private(set) var bottle = BottleState()
    @Published private(set) var now = Date.now
    @Published var toast: String?
    /// Goal changes over time, oldest first.
    @Published private(set) var goalHistory: [GoalChange] = []
    /// The first time the app was opened today, used to detect when the day started.
    @Published private(set) var firstActive: Date?

    private let bottleConnection = SimulatedBottle()
    private var liveTask: Task<Void, Never>?
    private var clockTask: Task<Void, Never>?
    private var toastTask: Task<Void, Never>?

    private static let settingsKey = "settings"
    private static let goalHistoryKey = "goalHistory"
    private static let firstActiveKey = "firstActive"

    init() {
        if let data = UserDefaults.standard.data(forKey: Self.settingsKey),
           let saved = try? JSONDecoder().decode(AppSettings.self, from: data) {
            settings = saved
        } else {
            settings = AppSettings()
        }
        let defaults = UserDefaults.standard
        if let data = defaults.data(forKey: Self.goalHistoryKey),
           let saved = try? JSONDecoder().decode([GoalChange].self, from: data), !saved.isEmpty {
            goalHistory = saved
        } else {
            goalHistory = [GoalChange(day: Hydration.sinceAlways, goalMl: settings.goalMl)]
        }
        firstActive = defaults.object(forKey: Self.firstActiveKey) as? Date
        markActive()

        bottleConnection.onStateChange = { [weak self] state in self?.bottle = state }
        // Sips measured by the bottle go straight to the backend
        bottleConnection.onSip = { [weak self] ml in self?.addDrink(milliliters: ml, fromBottle: true) }
        bottleConnection.connect()

        startLiveUpdates()
        clockTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(60))
                self?.now = .now
            }
        }
    }

    // MARK: Derived values

    private var api: DrinklyAPI { DrinklyAPI(baseUrl: settings.backendUrl) }
    var totals: [Date: Int] { Hydration.dailyTotals(entries) }
    var todayMl: Int { totals[Calendar.current.startOfDay(for: now)] ?? 0 }
    /// Share of the goal, may go above 1.
    var progress: Double { Double(todayMl) / Double(settings.goalMl) }
    var streak: Int { Hydration.streak(totals: totals, goal: goal(on:), today: now) }
    var longestStreak: Int { Hydration.longestStreak(totals: totals, goal: goal(on:)) }
    var dayStart: Date {
        Hydration.dayStart(now: now, wakeHour: settings.wakeHour, detectWakeUp: settings.startOnWakeUp,
                           firstActive: firstActive, entries: entries)
    }
    var expectedMl: Int {
        Hydration.expected(by: now, goalMl: settings.goalMl, start: dayStart, sleepHour: settings.sleepHour)
    }
    var message: HydrationMessage {
        Hydration.message(todayMl: todayMl, goalMl: settings.goalMl, expectedMl: expectedMl,
                          lastHourMl: Hydration.lastHour(entries, now: now), streak: streak)
    }
    var status: HydrationStatus { message.status }

    /// The goal that applied on `day` (the current one for today).
    func goal(on day: Date) -> Int {
        Hydration.goal(on: Calendar.current.startOfDay(for: day), history: goalHistory, fallback: settings.goalMl)
    }

    // MARK: Actions

    func refresh() {
        now = .now
        markActive()
        Task { await load() }
    }

    func addDrink(milliliters: Int, fromBottle: Bool = false) {
        Task {
            do {
                try await api.add(username: settings.username, milliliters: milliliters)
                await load()
                show(fromBottle ? "Bottle synced \(milliliters) ml" : "Added \(milliliters) ml")
            } catch {
                show("Couldn't save the drink.")
            }
        }
    }

    func deleteDrink(_ entry: DrinkEntry) {
        Task {
            do {
                try await api.delete(id: entry.id)
                await load()
                show("Removed \(entry.milliliters) ml")
            } catch {
                show("Couldn't remove the drink.")
            }
        }
    }

    func toggleBottle() {
        if bottle.link == .disconnected {
            bottleConnection.connect()
        } else {
            bottleConnection.disconnect()
        }
    }

    func simulateSip(milliliters: Int) {
        bottleConnection.simulateSip(milliliters: milliliters)
    }

    func sendTestReminder() {
        Task { await ReminderScheduler.sendTest(todayMl: todayMl, goalMl: settings.goalMl) }
    }

    // MARK: Internals

    private func settingsChanged(from old: AppSettings) {
        if let data = try? JSONEncoder().encode(settings) {
            UserDefaults.standard.set(data, forKey: Self.settingsKey)
        }
        if old.goalMl != settings.goalMl {
            goalHistory = Hydration.recordGoal(settings.goalMl, on: Calendar.current.startOfDay(for: .now), in: goalHistory)
            if let data = try? JSONEncoder().encode(goalHistory) {
                UserDefaults.standard.set(data, forKey: Self.goalHistoryKey)
            }
        }
        if old.backendUrl != settings.backendUrl || old.username != settings.username {
            startLiveUpdates()
        }
        if old.reminderPlan != settings.reminderPlan && settings.reminderPlan != .off {
            Task { await ReminderScheduler.requestPermission() }
        }
        rescheduleReminders()
    }

    /// Loads the drinks and then follows the backend's live stream, reconnecting when it drops.
    private func startLiveUpdates() {
        liveTask?.cancel()
        entries = []
        liveTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let api = self?.api else { return }
                do {
                    try await api.listen { [weak self] _ in
                        self?.isLive = true
                        await self?.load()
                    }
                } catch {}
                self?.isLive = false
                await self?.load()
                try? await Task.sleep(for: .seconds(3))
            }
        }
    }

    private func load() async {
        let settings = settings
        do {
            entries = try await DrinklyAPI(baseUrl: settings.backendUrl)
                .entries(for: settings.username)
                .sorted { $0.timestamp > $1.timestamp }
            errorMessage = nil
        } catch is CancellationError {
            return
        } catch {
            errorMessage = "Can't reach the backend at \(settings.backendUrl)"
        }
        rescheduleReminders()
    }

    /// Remembers the first time the app is used each day, as a sign that you got up.
    private func markActive() {
        let now = Date.now
        let calendar = Calendar.current
        if let firstActive, calendar.isDate(firstActive, inSameDayAs: now) { return }
        guard calendar.component(.hour, from: now) >= Hydration.earliestDayStartHour else { return }
        firstActive = now
        UserDefaults.standard.set(now, forKey: Self.firstActiveKey)
    }

    private func rescheduleReminders() {
        let settings = settings
        let todayMl = todayMl
        let dayStart = dayStart
        Task { await ReminderScheduler.reschedule(settings: settings, todayMl: todayMl, dayStart: dayStart) }
    }

    private func show(_ message: String) {
        toastTask?.cancel()
        withAnimation { toast = message }
        toastTask = Task {
            try? await Task.sleep(for: .seconds(2.5))
            guard !Task.isCancelled else { return }
            withAnimation { toast = nil }
        }
    }
}
