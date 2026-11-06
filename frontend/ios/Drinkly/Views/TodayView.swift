import SwiftUI

struct TodayView: View {
    @EnvironmentObject private var model: AppModel
    @State private var showAddSheet = false

    var body: some View {
        let status = model.status

        VStack(spacing: 0) {
            HStack {
                ScreenTitle("Today")
                Spacer()
                SyncButton()
            }
            if let error = model.errorMessage {
                Text(error)
                    .font(.footnote)
                    .foregroundStyle(Theme.warning)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Spacer(minLength: 16)

            Text("\(model.todayMl)")
                .font(.system(size: 96, weight: .medium))
                .tracking(-2)
                .monospacedDigit()
                .foregroundStyle(status.accent)
                .contentTransition(.numericText(value: Double(model.todayMl)))
            Text("of \(model.settings.goalMl) ml goal")
                .font(.callout)
                .foregroundStyle(Theme.muted)

            HStack(spacing: 8) {
                Chip(symbol: "drop.fill", text: status.label, color: status.accent)
                Chip(symbol: "flame.fill", text: "\(model.streak)-day streak", color: Theme.flame)
            }
            .padding(.top, 14)

            Button {
                showAddSheet = true
            } label: {
                Image(systemName: "plus")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Theme.navy)
                    .frame(width: 52, height: 52)
                    .background(.white, in: Circle())
                    .shadow(color: Theme.navy.opacity(0.2), radius: 8, y: 3)
            }
            .accessibilityLabel("Add a drink")
            .padding(.top, 20)

            Spacer(minLength: 16)
            Spacer(minLength: 0)

            BottleRow(bottle: model.bottle, now: model.now) { model.toggleBottle() }
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 16)
        .animation(.default, value: model.todayMl)
        .background {
            WaveBackground(level: 0.36 + 0.4 * model.progress, water: status.water)
                .animation(.spring(duration: 1.4), value: model.progress)
                .animation(.easeInOut, value: model.status)
                .ignoresSafeArea()
        }
        .sheet(isPresented: $showAddSheet) {
            AddDrinkSheet { ml in
                model.addDrink(milliliters: ml)
                showAddSheet = false
            }
            .presentationDetents([.height(380)])
            .presentationDragIndicator(.visible)
        }
    }
}

private struct SyncButton: View {
    @EnvironmentObject private var model: AppModel

    private var symbol: String {
        if model.errorMessage != nil { return "icloud.slash" }
        return model.isLive ? "checkmark.icloud" : "arrow.triangle.2.circlepath.icloud"
    }

    private var label: String {
        if model.errorMessage != nil { return "Backend offline, tap to retry" }
        return model.isLive ? "Live connection to the backend" : "Connecting to the backend"
    }

    var body: some View {
        Button {
            model.refresh()
        } label: {
            Image(systemName: symbol)
                .font(.title3)
                .foregroundStyle(Theme.blue)
                .frame(width: 44, height: 44)
        }
        .accessibilityLabel(label)
    }
}

private struct BottleRow: View {
    let bottle: BottleState
    let now: Date
    let onToggle: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            BottleIllustration(dimmed: bottle.link != .connected)
                .frame(width: 44, height: 110)
            VStack(alignment: .leading, spacing: 8) {
                Text(bottle.name)
                    .font(.footnote)
                    .foregroundStyle(Theme.muted)
                HStack(spacing: 12) {
                    Button(action: onToggle) {
                        InfoBadge(symbol: connectionSymbol, label: "Connection", value: connectionText)
                    }
                    .buttonStyle(.plain)
                    InfoBadge(symbol: batterySymbol, label: "Battery", value: bottle.batteryPercent.map { "\($0)%" } ?? "–")
                }
                Text(lastSyncText)
                    .font(.caption)
                    .foregroundStyle(Theme.muted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var connectionSymbol: String {
        switch bottle.link {
        case .connected: "antenna.radiowaves.left.and.right"
        case .connecting: "dot.radiowaves.left.and.right"
        case .disconnected: "antenna.radiowaves.left.and.right.slash"
        }
    }

    private var connectionText: String {
        switch bottle.link {
        case .connected: "Paired"
        case .connecting: "Pairing…"
        case .disconnected: "Off"
        }
    }

    private var batterySymbol: String {
        guard let percent = bottle.batteryPercent else { return "battery.0percent" }
        switch percent {
        case ..<15: return "battery.0percent"
        case ..<40: return "battery.25percent"
        case ..<65: return "battery.50percent"
        case ..<90: return "battery.75percent"
        default: return "battery.100percent"
        }
    }

    private var lastSyncText: String {
        guard let lastSync = bottle.lastSync else { return "Not synced yet" }
        if now.timeIntervalSince(lastSync) < 60 { return "Synced just now" }
        return "Synced " + RelativeDateTimeFormatter().localizedString(for: lastSync, relativeTo: now)
    }
}

private struct AddDrinkSheet: View {
    let onAdd: (Int) -> Void
    @State private var custom = 200

    init(onAdd: @escaping (Int) -> Void) {
        self.onAdd = onAdd
    }

    private let presets = [("Cup", 150), ("Glass", 250), ("Can", 330), ("Bottle", 500)]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Add a drink")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.ink)
            Text("Sips from your bottle are added automatically. Log other drinks here.")
                .font(.subheadline)
                .foregroundStyle(Theme.muted)

            HStack(spacing: 10) {
                ForEach(presets.indices, id: \.self) { index in
                    let (name, ml) = presets[index]
                    Button {
                        onAdd(ml)
                    } label: {
                        VStack(spacing: 2) {
                            Image(systemName: "drop.fill").foregroundStyle(Theme.blue)
                            Text("\(ml) ml").font(.subheadline.weight(.semibold)).foregroundStyle(Theme.ink)
                            Text(name).font(.caption).foregroundStyle(Theme.muted)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Theme.sky, in: RoundedRectangle(cornerRadius: 18))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.top, 18)

            HStack {
                Button { custom = max(custom - 50, 50) } label: {
                    Image(systemName: "minus").frame(width: 44, height: 44).background(Theme.sky, in: Circle())
                }
                .accessibilityLabel("50 ml less")
                Text("\(custom) ml")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(Theme.ink)
                    .frame(maxWidth: .infinity)
                Button { custom = min(custom + 50, 2000) } label: {
                    Image(systemName: "plus").frame(width: 44, height: 44).background(Theme.sky, in: Circle())
                }
                .accessibilityLabel("50 ml more")
            }
            .padding(.top, 22)

            Button {
                onAdd(custom)
            } label: {
                Text("Add \(custom) ml")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .buttonBorderShape(.capsule)
            .tint(Theme.blue)
            .padding(.top, 14)
        }
        .padding(24)
    }
}
