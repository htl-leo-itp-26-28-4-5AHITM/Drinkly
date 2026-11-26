import SwiftUI

struct TodayView: View {
    @EnvironmentObject private var model: AppModel
    @State private var showAddSheet = false
    @State private var showConnections = false

    var body: some View {
        let message = model.message
        let status = message.status

        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 0) {
                    Text("Today")
                        .font(.system(size: 30, weight: .semibold))
                        .foregroundStyle(Theme.blue)
                    Text(model.now.formatted(.dateTime.weekday(.wide).day().month(.wide)))
                        .font(.subheadline)
                        .foregroundStyle(Theme.muted)
                }
                .accessibilityElement(children: .combine)
                .accessibilityAddTraits(.isHeader)
                Spacer()
                ConnectionButton { showConnections = true }
            }
            .padding(.top, 12)

            Spacer(minLength: 16)

            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("\(model.todayMl)")
                    .font(.system(size: 88, weight: .semibold))
                    .tracking(-2)
                    .monospacedDigit()
                    .contentTransition(.numericText(value: Double(model.todayMl)))
                Text("ml")
                    .font(.system(size: 30, weight: .semibold))
            }
            .foregroundStyle(status.accent)
            .lineLimit(1)
            .minimumScaleFactor(0.6)
            .accessibilityElement(children: .combine)
            Text("of \(model.settings.goalMl) ml daily goal")
                .font(.callout.weight(.medium))
                .foregroundStyle(Theme.muted)

            StatusCard(message: message, streak: model.streak)
                .padding(.top, 20)

            Button {
                showAddSheet = true
            } label: {
                Label("Add a drink", systemImage: "plus")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 28)
                    .padding(.vertical, 15)
                    .background(Theme.blue, in: Capsule())
                    .shadow(color: Theme.blue.opacity(0.35), radius: 10, y: 4)
            }
            .buttonStyle(.pressable)
            .padding(.top, 18)

            Spacer(minLength: 16)
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 140) // room for the sand and the flower
        .animation(.default, value: model.todayMl)
        .background {
            GeometryReader { geo in
                HydrationScene(fill: model.progress, goalMl: model.settings.goalMl, water: status.water,
                               topInset: geo.safeAreaInsets.top, bottomInset: geo.safeAreaInsets.bottom)
            }
            .ignoresSafeArea()
            .animation(.spring(duration: 1.4), value: model.progress)
        }
        .sheet(isPresented: $showAddSheet) {
            AddDrinkSheet(todayMl: model.todayMl, goalMl: model.settings.goalMl,
                          lastHourMl: Hydration.lastHour(model.entries, now: .now)) { ml in
                model.addDrink(milliliters: ml)
                showAddSheet = false
            }
            .presentationDetents([.height(470)])
            .presentationDragIndicator(.visible)
            .presentationBackground(.white)
        }
        .sheet(isPresented: $showConnections) {
            ConnectionSheet()
                .presentationDetents([.height(320)])
                .presentationDragIndicator(.visible)
                .presentationBackground(.white)
        }
    }
}

/// What's going on right now plus the streak, in one card.
private struct StatusCard: View {
    let message: HydrationMessage
    let streak: Int

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: message.status.symbol)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(message.status.accent)
                .frame(width: 44, height: 44)
                .background(message.status.accent.opacity(0.14), in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(message.title).font(.headline).foregroundStyle(Theme.ink)
                Text(message.detail).font(.subheadline).foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)

            Rectangle().fill(Theme.line).frame(width: 1, height: 40)

            VStack(spacing: 0) {
                HStack(spacing: 3) {
                    Image(systemName: "flame.fill").font(.system(size: 15))
                    Text("\(streak)").font(.title3.weight(.bold)).monospacedDigit()
                }
                .foregroundStyle(streak > 0 ? Theme.blue : Theme.noData)
                Text(streak == 1 ? "day" : "days").font(.caption).foregroundStyle(Theme.muted)
            }
            .frame(minWidth: 44)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(streak)-day streak")
        }
        .padding(14)
        .background(.white.opacity(0.94), in: RoundedRectangle(cornerRadius: 22))
        .shadow(color: Theme.navy.opacity(0.08), radius: 12, y: 4)
    }
}

/// One indicator for everything the app is connected to; tap for details.
private struct ConnectionButton: View {
    @EnvironmentObject private var model: AppModel
    let action: () -> Void

    private var allGood: Bool { model.errorMessage == nil && model.isLive && model.bottle.link == .connected }

    private var text: String {
        if model.errorMessage != nil { return "Offline" }
        switch model.bottle.link {
        case .connected: return model.bottle.batteryPercent.map { "\($0) %" } ?? "Paired"
        case .connecting: return "Pairing…"
        case .disconnected: return "Bottle off"
        }
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: "waterbottle.fill").foregroundStyle(Theme.navy)
                Text(text).font(.footnote.weight(.semibold)).foregroundStyle(Theme.ink)
                Circle()
                    .fill(allGood ? Theme.blue : Theme.noData)
                    .frame(width: 8, height: 8)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 9)
            .background(.white, in: Capsule())
            .shadow(color: Theme.navy.opacity(0.1), radius: 8, y: 2)
        }
        .buttonStyle(.pressable)
        .accessibilityLabel("Connections: \(text)")
        .accessibilityHint("Shows the bottle and server status")
    }
}

private struct ConnectionSheet: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Connections")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.ink)

            ConnectionRow(symbol: "waterbottle.fill", title: model.bottle.name, detail: bottleDetail,
                          ok: model.bottle.link == .connected) {
                Button(model.bottle.link == .disconnected ? "Connect" : "Disconnect") { model.toggleBottle() }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.capsule)
                    .tint(Theme.blue)
            }

            Divider().overlay(Theme.line)

            ConnectionRow(symbol: "icloud.fill", title: "Drinkly server", detail: serverDetail,
                          ok: model.errorMessage == nil && model.isLive) {
                Button("Retry") { model.refresh() }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.capsule)
                    .tint(Theme.blue)
                    .opacity(model.errorMessage == nil ? 0 : 1)
            }
        }
        .padding(24)
    }

    private var bottleDetail: String {
        switch model.bottle.link {
        case .connected:
            let battery = model.bottle.batteryPercent.map { "\($0) % battery" } ?? "Paired"
            return battery + " · " + lastSyncText(model.bottle.lastSync, now: model.now)
        case .connecting: return "Pairing…"
        case .disconnected: return "Not connected"
        }
    }

    private var serverDetail: String {
        if let error = model.errorMessage { return error }
        return model.isLive ? "Live updates" : "Connecting…"
    }

    private func lastSyncText(_ lastSync: Date?, now: Date) -> String {
        guard let lastSync else { return "not synced yet" }
        if now.timeIntervalSince(lastSync) < 60 { return "synced just now" }
        return "synced " + RelativeDateTimeFormatter().localizedString(for: lastSync, relativeTo: now)
    }
}

private struct ConnectionRow<Action: View>: View {
    let symbol: String
    let title: String
    let detail: String
    let ok: Bool
    @ViewBuilder let action: Action

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: symbol)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(ok ? .white : Theme.navy)
                .frame(width: 44, height: 44)
                .background(ok ? Theme.blue : Theme.sky, in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(title).fontWeight(.semibold).foregroundStyle(Theme.ink)
                Text(detail).font(.footnote).foregroundStyle(Theme.muted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
            action
        }
    }
}

private struct AddDrinkSheet: View {
    let todayMl: Int
    let goalMl: Int
    let lastHourMl: Int
    let onAdd: (Int) -> Void
    @State private var custom = 200
    @State private var showInfo = false

    init(todayMl: Int, goalMl: Int, lastHourMl: Int, onAdd: @escaping (Int) -> Void) {
        self.todayMl = todayMl
        self.goalMl = goalMl
        self.lastHourMl = lastHourMl
        self.onAdd = onAdd
    }

    private let presets = [("Cup", 150), ("Glass", 250), ("Can", 330), ("Bottle", 500)]

    /// A gentle hint when the amount would be more than the body needs.
    private var warning: String? {
        if lastHourMl + custom > Hydration.maxPerHourMl {
            return "That's over 1 l within an hour. Better spread it out."
        }
        if Double(todayMl + custom) >= Double(goalMl) * Hydration.tooMuchFactor {
            return "That brings you to \(todayMl + custom) ml, well above your goal."
        }
        return nil
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                Text("Add a drink")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(Theme.ink)
                Button {
                    showInfo = true
                } label: {
                    Image(systemName: "info.circle")
                        .font(.title3)
                        .foregroundStyle(Theme.blue)
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel("About adding drinks")
                .popover(isPresented: $showInfo, arrowEdge: .top) {
                    Text("Sips from your Drinkly bottle are logged automatically. Add drinks from other glasses or bottles here.")
                        .font(.subheadline)
                        .foregroundStyle(Theme.ink)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(width: 260)
                        .padding()
                        .presentationCompactAdaptation(.popover)
                }
                Spacer()
            }

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
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Theme.water, lineWidth: 1))
                    }
                    .buttonStyle(.pressable)
                    .accessibilityLabel("Add \(name), \(ml) ml")
                }
            }
            .padding(.top, 10)

            HStack(alignment: .firstTextBaseline) {
                Text("Other amount").font(.subheadline.weight(.semibold)).foregroundStyle(Theme.ink)
                Spacer()
                Text("\(custom) ml")
                    .font(.title2.weight(.semibold))
                    .monospacedDigit()
                    .foregroundStyle(Theme.blue)
                    .contentTransition(.numericText(value: Double(custom)))
            }
            .padding(.top, 24)

            Slider(
                value: Binding(get: { Double(custom) }, set: { custom = Int($0) }),
                in: 50...1000,
                step: 50
            ) {
                Text("Amount")
            } minimumValueLabel: {
                Text("50").font(.caption).foregroundStyle(Theme.muted)
            } maximumValueLabel: {
                Text("1000").font(.caption).foregroundStyle(Theme.muted)
            }
            .tint(Theme.blue)
            .accessibilityValue("\(custom) ml")

            Group {
                if let warning {
                    Label(warning, systemImage: "hand.raised.fill")
                        .font(.footnote)
                        .foregroundStyle(Theme.navy)
                        .padding(10)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Theme.sky, in: RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(.top, 8)
            .animation(.default, value: warning)

            Spacer(minLength: 12)

            Button {
                onAdd(custom)
            } label: {
                Text("Add \(custom) ml")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Theme.blue, in: Capsule())
            }
            .buttonStyle(.pressable)
        }
        .padding(24)
        .animation(.default, value: custom)
    }
}
