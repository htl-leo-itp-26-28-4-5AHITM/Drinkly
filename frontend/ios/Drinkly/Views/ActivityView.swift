import Charts
import SwiftUI

private enum ActivityRange: String, CaseIterable, Identifiable {
    case week = "Week", month = "30 days", calendar = "Calendar"
    var id: Self { self }
}

struct ActivityView: View {
    @EnvironmentObject private var model: AppModel
    @State private var range = ActivityRange.week
    @State private var calendarMonth = Date.now

    private struct Day: Identifiable {
        let date: Date
        let ml: Int
        let goal: Int
        var id: Date { date }
        var hasData: Bool { ml > 0 }
    }

    var body: some View {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: model.now)
        let totals = model.totals
        let count = range == .month ? 30 : 7
        let days = (0..<count).reversed().map { offset in
            let date = calendar.date(byAdding: .day, value: -offset, to: today)!
            return Day(date: date, ml: totals[date] ?? 0, goal: model.goal(on: date))
        }
        let active = days.filter(\.hasData)

        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if range == .calendar {
                    CalendarCard(month: $calendarMonth) { rangePicker }
                        .padding(.top, 16)
                } else {
                    Card {
                        Text((days.first!.date..<days.last!.date).formatted(.interval.day().month(.wide).year()))
                            .font(.headline)
                            .foregroundStyle(Theme.ink)
                        chart(days)
                        rangePicker
                            .padding(.top, 6)
                    }
                    .padding(.top, 16)

                    Grid(horizontalSpacing: 14, verticalSpacing: 14) {
                        GridRow {
                            StatTile(label: "Daily average",
                                     value: active.isEmpty ? "–" : Liters.text(active.map(\.ml).reduce(0, +) / active.count))
                            StatTile(label: "Best day", value: Liters.text(days.map(\.ml).max() ?? 0))
                        }
                        GridRow {
                            StatTile(label: "Goal reached", value: "\(days.filter { $0.ml >= $0.goal }.count) of \(count) days")
                            StatTile(label: "Longest streak", value: "\(model.longestStreak) days")
                        }
                    }
                }

                Card(title: "Recent drinks") {
                    if model.entries.isEmpty {
                        Text("No drinks logged yet.").foregroundStyle(Theme.muted)
                    }
                    ForEach(Array(model.entries.prefix(20).enumerated()), id: \.element.id) { index, entry in
                        if index > 0 { Divider().overlay(Theme.line) }
                        DrinkRow(entry: entry) { model.deleteDrink(entry) }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
    }

    private var rangePicker: some View {
        Picker("Range", selection: $range) {
            ForEach(ActivityRange.allCases) { Text($0.rawValue).tag($0) }
        }
        .pickerStyle(.segmented)
    }

    private func chart(_ days: [Day]) -> some View {
        let yMax = Double(max(days.map(\.goal).max() ?? 0, days.map(\.ml).max() ?? 0)) * 1.1
        let yStep: Double = yMax > 3000 ? 1000 : 500
        // Every day for a week, every fifth day (counted back from today) for 30 days
        let labelled = days.enumerated()
            .filter { days.count <= 7 || (days.count - 1 - $0.offset) % 5 == 0 }
            .map(\.element.date)
        let withData = Set(days.filter(\.hasData).map(\.date))
        let currentGoal = Double(days.first?.goal ?? 0)

        return Chart {
            ForEach(days) { day in
                if day.hasData {
                    BarMark(x: .value("Day", day.date, unit: .day), y: .value("Intake", Double(day.ml)))
                        .foregroundStyle(day.ml >= day.goal ? Theme.blue : Theme.blue.opacity(0.45))
                        .cornerRadius(6)
                } else {
                    // Grayed-out column: the bottle wasn't used and nothing was logged
                    RectangleMark(x: .value("Day", day.date, unit: .day), yStart: .value("Intake", 0.0), yEnd: .value("Intake", yMax))
                        .foregroundStyle(Theme.noData.opacity(0.3))
                        .cornerRadius(6)
                }
            }
            ForEach(days) { day in
                LineMark(x: .value("Day", day.date, unit: .day), y: .value("Goal", Double(day.goal)))
                    .interpolationMethod(.stepCenter)
                    .foregroundStyle(Theme.navy.opacity(0.6))
                    .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
            }
        }
        .chartYScale(domain: 0...yMax)
        .chartYAxis {
            AxisMarks(position: .leading, values: .stride(by: yStep)) { value in
                AxisGridLine().foregroundStyle(Theme.line)
                AxisValueLabel {
                    let ml = value.as(Double.self) ?? 0
                    // Left out where the "Goal" tag sits
                    if abs(ml - currentGoal) > yStep * 0.3 {
                        Text(Liters.text(Int(ml))).foregroundStyle(Theme.muted)
                    }
                }
            }
            // "Goal" tag on the y-axis where the goal line starts
            AxisMarks(position: .leading, values: [currentGoal]) { _ in
                AxisValueLabel {
                    Text("Goal")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(Theme.sky) // contrasts with navy in light and dark mode
                        .padding(.horizontal, 5)
                        .padding(.vertical, 1)
                        .background(Theme.navy, in: Capsule())
                }
            }
        }
        .chartXAxis {
            AxisMarks(values: labelled) { value in
                let date = value.as(Date.self) ?? .now
                AxisValueLabel(centered: true) {
                    VStack(spacing: 0) {
                        if days.count <= 7 {
                            Text(date, format: .dateTime.weekday(.abbreviated))
                            Text(date, format: .dateTime.day())
                        } else {
                            Text(date, format: .dateTime.day().month(.abbreviated))
                        }
                    }
                    .foregroundStyle(withData.contains(date) ? Theme.ink : Theme.muted.opacity(0.6))
                }
            }
        }
        .frame(height: 210)
    }
}

/// A month at a glance: the stronger the blue, the closer you got to your goal that day.
private struct CalendarCard<Footer: View>: View {
    @EnvironmentObject private var model: AppModel
    @Binding var month: Date
    @ViewBuilder var footer: Footer

    var body: some View {
        let calendar = Calendar.current
        let interval = calendar.dateInterval(of: .month, for: month)!
        let first = interval.start
        let dayCount = calendar.range(of: .day, in: .month, for: first)!.count
        let leading = (calendar.component(.weekday, from: first) - calendar.firstWeekday + 7) % 7
        let today = calendar.startOfDay(for: model.now)
        let totals = model.totals
        let cells: [Date?] = Array(repeating: nil, count: leading)
            + (0..<dayCount).map { calendar.date(byAdding: .day, value: $0, to: first)! }
        let past = cells.compactMap { $0 }.filter { $0 <= today }
        let reached = past.filter { (totals[$0] ?? 0) >= model.goal(on: $0) }.count
        let symbols = calendar.veryShortStandaloneWeekdaySymbols
        let weekdays = Array(symbols[(calendar.firstWeekday - 1)...] + symbols[..<(calendar.firstWeekday - 1)])

        Card {
            HStack {
                Button { shift(-1) } label: {
                    Image(systemName: "chevron.left").frame(width: 44, height: 44)
                }
                .accessibilityLabel("Previous month")
                Spacer()
                Text(first, format: .dateTime.month(.wide).year())
                    .font(.headline)
                    .foregroundStyle(Theme.ink)
                Spacer()
                Button { shift(1) } label: {
                    Image(systemName: "chevron.right").frame(width: 44, height: 44)
                }
                .disabled(interval.contains(today))
                .accessibilityLabel("Next month")
            }
            .tint(Theme.blue)

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
                ForEach(weekdays.indices, id: \.self) { i in
                    Text(weekdays[i]).font(.caption.weight(.semibold)).foregroundStyle(Theme.muted)
                }
                ForEach(cells.indices, id: \.self) { i in
                    if let day = cells[i] {
                        DayCell(day: day, ml: totals[day] ?? 0, goal: model.goal(on: day),
                                isToday: day == today, isFuture: day > today)
                    } else {
                        Color.clear.frame(height: 38)
                    }
                }
            }

            Text("Goal reached on \(reached) of \(past.count) days")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.ink)
            footer
                .padding(.top, 6)
        }
    }

    private func shift(_ months: Int) {
        month = Calendar.current.date(byAdding: .month, value: months, to: month) ?? month
    }
}

private struct DayCell: View {
    let day: Date
    let ml: Int
    let goal: Int
    let isToday: Bool
    let isFuture: Bool

    var body: some View {
        let share = min(Double(ml) / Double(goal), 1)
        let fill: Color = isFuture ? .clear : ml == 0 ? Theme.noData.opacity(0.35) : Theme.blue.opacity(0.2 + 0.8 * share)
        let text: Color = isFuture ? Theme.muted.opacity(0.5) : ml > 0 && share > 0.5 ? .white : ml == 0 ? Theme.muted : Theme.ink

        Text(day, format: .dateTime.day())
            .font(.footnote.weight(.semibold))
            .foregroundStyle(text)
            .frame(maxWidth: .infinity)
            .frame(height: 38)
            .background(fill, in: Circle())
            .overlay(Circle().stroke(Theme.navy, lineWidth: isToday ? 2 : 0))
            .accessibilityLabel(day.formatted(.dateTime.day().month(.wide)) + (ml > 0 ? ", \(Liters.text(ml))" : ", no drinks"))
    }
}

private struct StatTile: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.footnote).foregroundStyle(Theme.muted)
            Text(value).font(.title3.weight(.semibold)).foregroundStyle(Theme.ink)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Theme.card, in: RoundedRectangle(cornerRadius: 22))
        .accessibilityElement(children: .combine)
    }
}

private struct DrinkRow: View {
    let entry: DrinkEntry
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "drop.fill")
                .foregroundStyle(Theme.blue)
                .frame(width: 38, height: 38)
                .background(Theme.sky, in: Circle())
            VStack(alignment: .leading, spacing: 0) {
                Text(Liters.text(entry.milliliters)).fontWeight(.semibold).foregroundStyle(Theme.ink)
                Text(entry.timestamp.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption)
                    .foregroundStyle(Theme.muted)
            }
            Spacer()
            Button(action: onDelete) {
                Image(systemName: "trash")
                    .foregroundStyle(Theme.muted)
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel("Remove \(Liters.text(entry.milliliters))")
        }
    }
}
