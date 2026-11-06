import Charts
import SwiftUI

struct ActivityView: View {
    @EnvironmentObject private var model: AppModel
    @State private var range = 7

    private struct Day: Identifiable {
        let date: Date
        let ml: Int
        var id: Date { date }
    }

    var body: some View {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: model.now)
        let totals = model.totals
        let goal = model.settings.goalMl
        let days = (0..<range).reversed().map { offset in
            let date = calendar.date(byAdding: .day, value: -offset, to: today)!
            return Day(date: date, ml: totals[date] ?? 0)
        }
        let active = days.map(\.ml).filter { $0 > 0 }

        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                ScreenTitle("Activity", subtitle: "Your hydration over time")

                Picker("Range", selection: $range) {
                    Text("7 days").tag(7)
                    Text("30 days").tag(30)
                }
                .pickerStyle(.segmented)

                Card(title: "Daily intake") {
                    Chart {
                        ForEach(days) { day in
                            BarMark(
                                x: .value("Day", day.date, unit: .day),
                                y: .value("Millilitres", day.ml)
                            )
                            .foregroundStyle(day.ml >= goal ? Theme.blue : Theme.blue.opacity(0.45))
                            .cornerRadius(6)
                        }
                        RuleMark(y: .value("Goal", goal))
                            .foregroundStyle(Theme.navy.opacity(0.55))
                            .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
                    }
                    .chartXAxis {
                        AxisMarks(values: .stride(by: .day, count: range == 7 ? 1 : 5)) { _ in
                            AxisValueLabel(format: range == 7 ? Date.FormatStyle().weekday(.abbreviated) : Date.FormatStyle().day())
                        }
                    }
                    .chartYAxis(.hidden)
                    .frame(height: 190)

                    Text("Dashed line: your \(goal) ml goal")
                        .font(.caption)
                        .foregroundStyle(Theme.muted)
                }

                Grid(horizontalSpacing: 14, verticalSpacing: 14) {
                    GridRow {
                        StatTile(label: "Daily average",
                                 value: active.isEmpty ? "–" : "\(active.reduce(0, +) / active.count) ml")
                        StatTile(label: "Best day", value: "\(days.map(\.ml).max() ?? 0) ml")
                    }
                    GridRow {
                        StatTile(label: "Goal reached", value: "\(days.filter { $0.ml >= goal }.count) of \(range) days")
                        StatTile(label: "Longest streak", value: "\(model.longestStreak) days")
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
        .background(.white, in: RoundedRectangle(cornerRadius: 22))
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
                Text("\(entry.milliliters) ml").fontWeight(.semibold).foregroundStyle(Theme.ink)
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
            .accessibilityLabel("Remove \(entry.milliliters) ml")
        }
    }
}
