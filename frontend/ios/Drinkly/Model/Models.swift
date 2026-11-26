import Foundation

/// One drink as stored by the backend (`DrinkingRecord`). The backend stores litres.
struct DrinkEntry: Identifiable, Hashable {
    let id: Int
    let timestamp: Date
    let liters: Double

    var milliliters: Int { Int((liters * 1000).rounded()) }
}

enum ReminderPlan: String, CaseIterable, Identifiable, Codable {
    case off, relaxed, balanced, steady

    var id: String { rawValue }

    var title: String {
        switch self {
        case .off: "Off"
        case .relaxed: "Relaxed"
        case .balanced: "Balanced"
        case .steady: "Steady"
        }
    }

    var description: String {
        switch self {
        case .off: "No reminders"
        case .relaxed: "A nudge every 3 hours"
        case .balanced: "A nudge every 2 hours"
        case .steady: "A nudge every hour"
        }
    }

    var intervalHours: Int? {
        switch self {
        case .off: nil
        case .relaxed: 3
        case .balanced: 2
        case .steady: 1
        }
    }

    var symbol: String {
        switch self {
        case .off: "bell.slash.fill"
        case .relaxed: "leaf.fill"
        case .balanced: "scalemass.fill"
        case .steady: "bolt.fill"
        }
    }
}

struct AppSettings: Codable, Equatable {
    var username = "alice"
    var goalMl = 2000
    var weightKg = 70
    // The simulator shares the Mac's network, so localhost reaches the backend there
    var backendUrl = "http://localhost:8080"
    var reminderPlan = ReminderPlan.balanced
    var wakeHour = 7
    var sleepHour = 22
    var onlyWhenBehind = true
    /// Start the day when you get up (first use of the app or bottle) instead of at `wakeHour`.
    var startOnWakeUp = true

    init() {}

    /// Missing keys fall back to the defaults, so settings saved by older versions still load.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let d = AppSettings()
        username = try c.decodeIfPresent(String.self, forKey: .username) ?? d.username
        goalMl = try c.decodeIfPresent(Int.self, forKey: .goalMl) ?? d.goalMl
        weightKg = try c.decodeIfPresent(Int.self, forKey: .weightKg) ?? d.weightKg
        backendUrl = try c.decodeIfPresent(String.self, forKey: .backendUrl) ?? d.backendUrl
        reminderPlan = try c.decodeIfPresent(ReminderPlan.self, forKey: .reminderPlan) ?? d.reminderPlan
        wakeHour = try c.decodeIfPresent(Int.self, forKey: .wakeHour) ?? d.wakeHour
        sleepHour = try c.decodeIfPresent(Int.self, forKey: .sleepHour) ?? d.sleepHour
        onlyWhenBehind = try c.decodeIfPresent(Bool.self, forKey: .onlyWhenBehind) ?? d.onlyWhenBehind
        startOnWakeUp = try c.decodeIfPresent(Bool.self, forKey: .startOnWakeUp) ?? d.startOnWakeUp
    }
}

/// The daily goal from `day` on, so past days are judged by the goal that applied back then.
struct GoalChange: Codable, Equatable {
    let day: Date
    let goalMl: Int
}

enum HydrationStatus {
    case goalReached, onTrack, slightlyBehind, behind, tooMuch

    var label: String {
        switch self {
        case .goalReached: "Goal reached"
        case .onTrack: "On track"
        case .slightlyBehind: "A little behind"
        case .behind: "Time for a drink"
        case .tooMuch: "Take it easy"
        }
    }
}

/// The headline on the Today screen: what's going on and what to do next.
struct HydrationMessage: Equatable {
    let status: HydrationStatus
    let title: String
    let detail: String
}
