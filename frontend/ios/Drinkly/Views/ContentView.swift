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
        Group {
            switch tab {
            case .today: TodayView()
            case .activity: ActivityView()
            case .reminders: RemindersView()
            case .profile: ProfileView()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Theme.sky.ignoresSafeArea())
        .safeAreaInset(edge: .bottom, spacing: 0) {
            VStack(spacing: 10) {
                if let toast = model.toast {
                    Label {
                        Text(toast).foregroundStyle(Theme.ink)
                    } icon: {
                        Image(systemName: "drop.circle.fill").foregroundStyle(Theme.blue)
                    }
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 11)
                    .background(.white, in: Capsule())
                    .overlay(Capsule().stroke(Theme.water, lineWidth: 1))
                    .shadow(color: Theme.navy.opacity(0.15), radius: 12, y: 4)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                TabBar(selection: $tab)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 4)
        }
    }
}

private struct TabBar: View {
    @Binding var selection: Tab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Tab.allCases, id: \.self) { tab in
                let selected = tab == selection
                Button {
                    selection = tab
                } label: {
                    VStack(spacing: 3) {
                        Image(systemName: tab.symbol)
                            .font(.system(size: 20))
                            .frame(height: 24)
                        Text(tab.title)
                            .font(.system(size: 11, weight: selected ? .semibold : .regular))
                    }
                    .foregroundStyle(selected ? Theme.blue : Theme.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(selected ? Theme.sky : .clear, in: Capsule())
                    .contentShape(Capsule())
                }
                .buttonStyle(.pressable)
                .accessibilityAddTraits(selected ? [.isSelected] : [])
            }
        }
        .padding(6)
        .background(.white, in: Capsule())
        .shadow(color: Theme.navy.opacity(0.15), radius: 16, y: 6)
    }
}
