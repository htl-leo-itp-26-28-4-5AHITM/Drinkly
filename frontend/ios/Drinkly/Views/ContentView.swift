import SwiftUI

enum Tab: CaseIterable {
    case today, activity, reminders, profile

    var title: String {
        switch self {
        case .today: "Today"
        case .activity: "Activity"
        case .reminders: "Reminders"
        case .profile: "Profile"
        }
    }

    var symbol: String {
        switch self {
        case .today: "drop.fill"
        case .activity: "chart.bar.fill"
        case .reminders: "bell.badge.fill"
        case .profile: "person.fill"
        }
    }
}

struct ContentView: View {
    @EnvironmentObject private var model: AppModel
    @State private var tab = Tab.today

    var body: some View {
        // The system tab bar: Liquid Glass on iOS 26
        TabView(selection: $tab) {
            ForEach(Tab.allCases, id: \.self) { tab in
                page(tab)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Theme.sky.ignoresSafeArea())
                    .tabItem { Label(tab.title, systemImage: tab.symbol) }
                    .tag(tab)
            }
        }
        .tint(Theme.blue)
        .overlay(alignment: .top) {
            if let added = model.added {
                AddedNotice(drink: added) { model.undo(added) }
                    .padding(.top, 8)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .overlay(alignment: .bottom) {
            if let toast = model.toast {
                Label {
                    Text(toast).foregroundStyle(Theme.ink)
                } icon: {
                    Image(systemName: "drop.fill").foregroundStyle(Theme.blue)
                }
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 16)
                .padding(.vertical, 11)
                .glassSurface(in: Capsule())
                .padding(.bottom, 64) // above the tab bar
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }

    @ViewBuilder
    private func page(_ tab: Tab) -> some View {
        switch tab {
        case .today: TodayView()
        case .activity: ActivityView()
        case .reminders: RemindersView()
        case .profile: ProfileView()
        }
    }
}

/// Small pill from the top after a drink was saved, with a plain-text "Undo".
private struct AddedNotice: View {
    let drink: AddedDrink
    let onUndo: () -> Void

    var body: some View {
        let amount = Liters.text(drink.entry.milliliters)
        HStack(spacing: 6) {
            Image(systemName: "drop.fill")
                .font(.footnote)
                .foregroundStyle(Theme.blue)
            Text(drink.fromBottle ? "Synced \(amount)" : "Added \(amount)")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Theme.ink)
            Button("Undo", action: onUndo)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.blue)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .contentShape(Rectangle())
                .buttonStyle(.plain)
        }
        .padding(.leading, 14)
        .glassSurface(in: Capsule(), shadowRadius: 8)
        .accessibilityElement(children: .combine)
        .accessibilityAction(named: "Undo", onUndo)
    }
}
