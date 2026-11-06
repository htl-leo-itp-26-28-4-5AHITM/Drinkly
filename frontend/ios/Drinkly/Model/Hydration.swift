import Foundation

enum Hydration {

    /// Millilitres per day, keyed by the start of the day.
    static func dailyTotals(_ entries: [DrinkEntry], calendar: Calendar = .current) -> [Date: Int] {
        Dictionary(grouping: entries) { calendar.startOfDay(for: $0.timestamp) }
            .mapValues { drinks in drinks.reduce(0) { $0 + $1.milliliters } }
    }

    /// Days in a row the goal was met. Today only counts once it is reached, so an open day doesn't break the streak.
    static func streak(totals: [Date: Int], goalMl: Int, today: Date, calendar: Calendar = .current) -> Int {
        var day = calendar.startOfDay(for: today)
        if (totals[day] ?? 0) < goalMl {
            day = calendar.date(byAdding: .day, value: -1, to: day)!
        }
        var count = 0
        while (totals[day] ?? 0) >= goalMl {
            count += 1
            day = calendar.date(byAdding: .day, value: -1, to: day)!
        }
        return count
    }

    static func longestStreak(totals: [Date: Int], goalMl: Int, calendar: Calendar = .current) -> Int {
        var best = 0
        for day in totals.keys {
            let previous = calendar.date(byAdding: .day, value: -1, to: day)!
            if (totals[previous] ?? 0) >= goalMl { continue } // not the start of a run
            var run = 0
            var d = day
            while (totals[d] ?? 0) >= goalMl {
                run += 1
                d = calendar.date(byAdding: .day, value: 1, to: d)!
            }
            best = max(best, run)
        }
        return best
    }

    /// How much should have been drunk by `date` if the goal is spread evenly over the waking hours.
    static func expected(by date: Date, goalMl: Int, wakeHour: Int, sleepHour: Int, calendar: Calendar = .current) -> Int {
        let parts = calendar.dateComponents([.hour, .minute], from: date)
        let minute = (parts.hour ?? 0) * 60 + (parts.minute ?? 0)
        let start = wakeHour * 60
        let end = sleepHour * 60
        let fraction: Double
        if end <= start || minute <= start {
            fraction = 0
        } else if minute >= end {
            fraction = 1
        } else {
            fraction = Double(minute - start) / Double(end - start)
        }
        return Int((Double(goalMl) * fraction).rounded())
    }

    static func status(todayMl: Int, goalMl: Int, now: Date, wakeHour: Int, sleepHour: Int) -> HydrationStatus {
        if todayMl >= goalMl { return .goalReached }
        let expected = expected(by: now, goalMl: goalMl, wakeHour: wakeHour, sleepHour: sleepHour)
        if todayMl >= expected { return .onTrack }
        return Double(todayMl) >= Double(expected) * 0.6 ? .slightlyBehind : .behind
    }

    /// Common rule of thumb: about 35 ml per kg of body weight, rounded to 50 ml.
    static func suggestedGoal(weightKg: Int) -> Int {
        Int((Double(weightKg * 35) / 50).rounded()) * 50
    }
}
