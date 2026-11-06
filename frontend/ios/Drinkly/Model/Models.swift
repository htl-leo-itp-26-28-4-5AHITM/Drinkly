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
}

enum HydrationStatus {
    case goalReached, onTrack, slightlyBehind, behind

    var label: String {
        switch self {
        case .goalReached: "Goal reached"
        case .onTrack: "Well hydrated"
        case .slightlyBehind: "Slightly behind"
        case .behind: "Time for a drink"
        }
    }
}
