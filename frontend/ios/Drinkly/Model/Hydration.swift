import Foundation

enum Hydration {

    /// Above this share of the goal, more water doesn't help any more.
    static let tooMuchFactor = 1.5
    /// The kidneys can only handle about a litre per hour; drinking faster than that is risky.
    static let maxPerHourMl = 1000
    /// Drinks and app use before this hour still belong to the night, not to a new day.
    static let earliestDayStartHour = 4

    /// Millilitres per day, keyed by the start of the day.
    static func dailyTotals(_ entries: [DrinkEntry], calendar: Calendar = .current) -> [Date: Int] {
        Dictionary(grouping: entries) { calendar.startOfDay(for: $0.timestamp) }
            .mapValues { drinks in drinks.reduce(0) { $0 + $1.milliliters } }
    }

    // MARK: Goal history

    /// The goal that applied on `day`. Days before the first recorded change use the oldest known goal.
    static func goal(on day: Date, history: [GoalChange], fallback: Int) -> Int {
        history.last { $0.day <= day }?.goalMl ?? history.first?.goalMl ?? fallback
    }

    /// Records `goalMl` as the goal from `day` on; several changes on one day keep only the last.
    static func recordGoal(_ goalMl: Int, on day: Date, in history: [GoalChange]) -> [GoalChange] {
        var result = history.filter { $0.day < day }
        if result.last?.goalMl != goalMl {
            result.append(GoalChange(day: day, goalMl: goalMl))
        }
        return result
    }

    // MARK: Streaks

    /// Days in a row the goal was met. Today only counts once it is reached, so an open day doesn't break the streak.
    static func streak(totals: [Date: Int], goal: (Date) -> Int, today: Date, calendar: Calendar = .current) -> Int {
        var day = calendar.startOfDay(for: today)
        if (totals[day] ?? 0) < goal(day) {
            day = calendar.date(byAdding: .day, value: -1, to: day)!
        }
        var count = 0
        while (totals[day] ?? 0) >= goal(day) {
            count += 1
            day = calendar.date(byAdding: .day, value: -1, to: day)!
        }
        return count
    }

    static func longestStreak(totals: [Date: Int], goal: (Date) -> Int, calendar: Calendar = .current) -> Int {
        func reached(_ day: Date) -> Bool { (totals[day] ?? 0) >= goal(day) }
        var best = 0
        for day in totals.keys {
            if reached(calendar.date(byAdding: .day, value: -1, to: day)!) { continue } // not the start of a run
            var run = 0
            var d = day
            while reached(d) {
                run += 1
                d = calendar.date(byAdding: .day, value: 1, to: d)!
            }
            best = max(best, run)
        }
        return best
    }

    // MARK: Pace

    /// When today started: the first sign of life (app opened or a drink logged) after `earliestDayStartHour`,
    /// or the fixed wake-up hour if that detection is off or nothing happened yet.
    static func dayStart(now: Date, wakeHour: Int, detectWakeUp: Bool, firstActive: Date?, entries: [DrinkEntry],
                         calendar: Calendar = .current) -> Date {
        let fixed = calendar.date(bySettingHour: wakeHour, minute: 0, second: 0, of: now)!
        guard detectWakeUp else { return fixed }
        let earliest = calendar.date(bySettingHour: earliestDayStartHour, minute: 0, second: 0, of: now)!
        let signs = entries.map(\.timestamp) + [firstActive].compactMap { $0 }
        return signs.filter { $0 >= earliest && calendar.isDate($0, inSameDayAs: now) }.min() ?? fixed
    }

    /// How much should have been drunk by `date` if the goal is spread evenly between `start` and the sleep hour.
    static func expected(by date: Date, goalMl: Int, start: Date, sleepHour: Int, calendar: Calendar = .current) -> Int {
        let minute = minuteOfDay(date, calendar)
        let startMinute = minuteOfDay(start, calendar)
        let end = sleepHour * 60
        let fraction: Double
        if end <= startMinute || minute <= startMinute {
            fraction = 0
        } else if minute >= end {
            fraction = 1
        } else {
            fraction = Double(minute - startMinute) / Double(end - startMinute)
        }
        return Int((Double(goalMl) * fraction).rounded())
    }

    /// Millilitres drunk in the hour before `now`. No upper bound: the server stamps the drinks,
    /// and its clock may run slightly ahead of the phone's.
    static func lastHour(_ entries: [DrinkEntry], now: Date) -> Int {
        entries.filter { $0.timestamp > now.addingTimeInterval(-3600) }.reduce(0) { $0 + $1.milliliters }
    }

    static func status(todayMl: Int, goalMl: Int, expectedMl: Int, lastHourMl: Int) -> HydrationStatus {
        if lastHourMl > maxPerHourMl || Double(todayMl) >= Double(goalMl) * tooMuchFactor { return .tooMuch }
        if todayMl >= goalMl { return .goalReached }
        if todayMl >= expectedMl { return .onTrack }
        return Double(todayMl) >= Double(expectedMl) * 0.6 ? .slightlyBehind : .behind
    }

    static func message(todayMl: Int, goalMl: Int, expectedMl: Int, lastHourMl: Int, streak: Int) -> HydrationMessage {
        let status = status(todayMl: todayMl, goalMl: goalMl, expectedMl: expectedMl, lastHourMl: lastHourMl)
        let behind = roundUp(expectedMl - todayMl)
        switch status {
        case .tooMuch where lastHourMl > maxPerHourMl:
            return .init(status: status, title: "Slow down",
                         detail: "\(lastHourMl) ml in the last hour. Your body can take about 1 l per hour.")
        case .tooMuch:
            return .init(status: status, title: "That's plenty",
                         detail: "You're \(todayMl - goalMl) ml over your goal. No need for more today.")
        case .goalReached:
            return .init(status: status, title: "Goal reached",
                         detail: streak > 1 ? "Well done, that's \(streak) days in a row!" : "Well done! Come back tomorrow.")
        case .onTrack where todayMl == 0:
            return .init(status: status, title: "Let's get started",
                         detail: "A glass of water is a good first step.")
        case .onTrack:
            return .init(status: status, title: "On track", detail: "\(goalMl - todayMl) ml to go today.")
        case .slightlyBehind:
            return .init(status: status, title: "A little behind", detail: "Drink about \(behind) ml to catch up.")
        case .behind:
            return .init(status: status, title: "Time for a drink", detail: "You're \(behind) ml behind your pace.")
        }
    }

    /// Common rule of thumb: about 35 ml per kg of body weight, rounded to 50 ml.
    static func suggestedGoal(weightKg: Int) -> Int {
        Int((Double(weightKg * 35) / 50).rounded()) * 50
    }

    private static func minuteOfDay(_ date: Date, _ calendar: Calendar) -> Int {
        let parts = calendar.dateComponents([.hour, .minute], from: date)
        return (parts.hour ?? 0) * 60 + (parts.minute ?? 0)
    }

    /// Up to the next 50 ml, so suggestions read like amounts you'd actually pour.
    private static func roundUp(_ ml: Int) -> Int {
        max(50, Int((Double(ml) / 50).rounded(.up)) * 50)
    }
}
