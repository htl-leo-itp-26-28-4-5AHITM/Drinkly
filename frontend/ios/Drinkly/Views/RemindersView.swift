import SwiftUI

struct RemindersView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                ScreenTitle("Reminders", subtitle: "Pick a plan for your drink reminders")

                ForEach(ReminderPlan.allCases) { plan in
                    PlanCard(plan: plan, selected: plan == model.settings.reminderPlan) {
                        model.settings.reminderPlan = plan
                    }
                }

                Card(title: "Active hours") {
                    HourStepper(label: "From", hour: $model.settings.wakeHour, range: 4...(model.settings.sleepHour - 1))
                    HourStepper(label: "Until", hour: $model.settings.sleepHour, range: (model.settings.wakeHour + 1)...24)
                    Text("Reminders only arrive in this window. Your hydration status also uses it to pace the daily goal.")
                        .font(.footnote)
                        .foregroundStyle(Theme.muted)
                }

                Card {
                    Toggle(isOn: $model.settings.onlyWhenBehind) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Only when I'm behind").fontWeight(.semibold).foregroundStyle(Theme.ink)
                            Text("Skip reminders while you're on track").font(.footnote).foregroundStyle(Theme.muted)
                        }
                    }
                    .tint(Theme.blue)
                }

                Button {
                    model.sendTestReminder()
                } label: {
                    Text("Send a test reminder")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
                .tint(Theme.blue)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
    }
}

private struct PlanCard: View {
    let plan: ReminderPlan
    let selected: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 14) {
                Image(systemName: plan.symbol)
                    .foregroundStyle(selected ? .white : Theme.blue)
                    .frame(width: 42, height: 42)
                    .background(selected ? Theme.blue : Theme.sky, in: Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(plan.title).fontWeight(.semibold).foregroundStyle(Theme.ink)
                    Text(plan.description).font(.footnote).foregroundStyle(Theme.muted)
                }
                Spacer()
                Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(selected ? Theme.blue : Theme.line)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(.white, in: RoundedRectangle(cornerRadius: 22))
            .overlay {
                RoundedRectangle(cornerRadius: 22).stroke(selected ? Theme.blue : .clear, lineWidth: 2)
            }
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? [.isSelected] : [])
    }
}

private struct HourStepper: View {
    let label: String
    @Binding var hour: Int
    let range: ClosedRange<Int>

    var body: some View {
        Stepper(value: $hour, in: range) {
            HStack {
                Text(label).foregroundStyle(Theme.ink)
                Spacer()
                Text(String(format: "%02d:00", hour))
                    .fontWeight(.semibold)
                    .monospacedDigit()
                    .foregroundStyle(Theme.ink)
            }
        }
    }
}
