import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let settings = model.settings

        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                ScreenTitle("Profile")

                HStack(spacing: 16) {
                    Text(settings.username.prefix(2).uppercased())
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(.white)
                        .frame(width: 64, height: 64)
                        .background(Theme.blue, in: Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(settings.username).font(.title2.weight(.semibold)).foregroundStyle(Theme.ink)
                        Text("\(model.streak)-day streak · \(settings.goalMl) ml goal").foregroundStyle(Theme.muted)
                    }
                }

                Card(title: "Name") {
                    CommitTextField(value: settings.username) { model.settings.username = $0 }
                    Text("Your drinks are stored under this name in the backend.")
                        .font(.footnote)
                        .foregroundStyle(Theme.muted)
                }

                Card(title: "Daily goal") {
                    Text("\(settings.goalMl) ml")
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(Theme.blue)
                    Slider(
                        value: Binding(
                            get: { Double(model.settings.goalMl) },
                            set: { model.settings.goalMl = Int($0) }
                        ),
                        in: 1000...4000,
                        step: 50
                    )
                    .tint(Theme.blue)
                    Divider().overlay(Theme.line)
                    Stepper(value: $model.settings.weightKg, in: 30...200) {
                        HStack {
                            Text("Body weight").foregroundStyle(Theme.ink)
                            Spacer()
                            Text("\(settings.weightKg) kg").fontWeight(.semibold).foregroundStyle(Theme.ink)
                        }
                    }
                    let suggested = Hydration.suggestedGoal(weightKg: settings.weightKg)
                    HStack {
                        Text("Suggested: \(suggested) ml").font(.footnote).foregroundStyle(Theme.muted)
                        Spacer()
                        Button("Use as goal") {
                            model.settings.goalMl = min(max(suggested, 1000), 4000)
                        }
                        .disabled(suggested == settings.goalMl)
                        .tint(Theme.blue)
                    }
                }

                Card(title: "Bottle") {
                    HStack(spacing: 14) {
                        BottleIllustration(dimmed: model.bottle.link != .connected)
                            .frame(width: 28, height: 70)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(model.bottle.name).fontWeight(.semibold).foregroundStyle(Theme.ink)
                            Text(bottleStatus).font(.footnote).foregroundStyle(Theme.muted)
                        }
                        Spacer()
                        Button(model.bottle.link == .disconnected ? "Connect" : "Disconnect") { model.toggleBottle() }
                            .buttonStyle(.borderedProminent)
                            .buttonBorderShape(.capsule)
                            .tint(Theme.blue)
                    }
                    if model.bottle.link == .connected {
                        Button {
                            model.simulateSip(milliliters: 250)
                        } label: {
                            Text("Simulate a 250 ml sip").frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .buttonBorderShape(.capsule)
                        .tint(Theme.blue)
                    }
                    Text("Demo mode: the bottle is simulated until its Bluetooth profile is available.")
                        .font(.caption)
                        .foregroundStyle(Theme.muted)
                }

                Card(title: "Server") {
                    CommitTextField(value: settings.backendUrl, isURL: true) { model.settings.backendUrl = $0 }
                    HStack(spacing: 8) {
                        Circle().fill(serverColor).frame(width: 8, height: 8)
                        Text(serverText).font(.footnote).foregroundStyle(Theme.muted)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private var bottleStatus: String {
        switch model.bottle.link {
        case .connected: "Paired · battery \(model.bottle.batteryPercent ?? 0)%"
        case .connecting: "Pairing…"
        case .disconnected: "Not connected"
        }
    }

    private var serverColor: Color {
        if model.errorMessage != nil { return Theme.warning }
        return model.isLive ? Theme.success : Theme.muted
    }

    private var serverText: String {
        if model.errorMessage != nil { return "Not reachable" }
        return model.isLive ? "Live connection" : "Connecting…"
    }
}

/// Text field that only saves when confirmed, so the backend isn't re-queried on every keystroke.
private struct CommitTextField: View {
    let value: String
    var isURL = false
    let onCommit: (String) -> Void

    @State private var text = ""

    init(value: String, isURL: Bool = false, onCommit: @escaping (String) -> Void) {
        self.value = value
        self.isURL = isURL
        self.onCommit = onCommit
    }

    private var trimmed: String { text.trimmingCharacters(in: .whitespaces) }
    private var changed: Bool { !trimmed.isEmpty && trimmed != value }

    var body: some View {
        HStack {
            TextField("", text: $text)
                .keyboardType(isURL ? .URL : .default)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .onSubmit { if changed { onCommit(trimmed) } }
            if changed {
                Button { onCommit(trimmed) } label: {
                    Image(systemName: "checkmark.circle.fill").font(.title3).foregroundStyle(Theme.blue)
                }
                .accessibilityLabel("Save")
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Theme.sky, in: RoundedRectangle(cornerRadius: 14))
        .onAppear { text = value }
        .onChange(of: value) { _, new in text = new }
    }
}
