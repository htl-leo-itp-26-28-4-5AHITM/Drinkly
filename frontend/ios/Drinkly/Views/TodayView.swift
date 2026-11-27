import SwiftUI

struct TodayView: View {
    @EnvironmentObject private var model: AppModel
    @State private var showAddSheet = false
    @State private var intro: Double = 1
    @State private var introRunning = false

    /// 0…1 while the number counts up after opening the app, 1 otherwise.
    private var introFactor: Double {
        if model.introPlayed == model.introRequest { return 1 }
        // Requested but not started yet: show 0 rather than the old value for a frame
        return introRunning ? intro : 0
    }

    var body: some View {
        let status = model.status

        GeometryReader { geo in
            ZStack {
                HydrationScene(fill: model.progress * introFactor, water: status.water, topInset: geo.safeAreaInsets.top)
                    .animation(.spring(duration: 1.4), value: model.progress)
                    .ignoresSafeArea()

                // Hero: today's amount with the streak under it, in the vertical middle of the whole screen
                VStack(spacing: 0) {
                    HeroNumber(ml: Double(model.todayMl) * introFactor, color: status.accent)
                        .animation(.smooth(duration: 0.6), value: model.todayMl)
                    StreakLine(streak: model.streak)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .ignoresSafeArea()

                VStack(alignment: .leading, spacing: 24) {
                    HStack {
                        BottlePill()
                        Spacer()
                        CloudButton()
                    }
                    Text("\(greeting(model.now)), \(model.settings.username)")
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundStyle(Theme.navy)
                        .accessibilityAddTraits(.isHeader)
                    Spacer()
                    // The main action, within thumb reach above the tab bar
                    AddButton { showAddSheet = true }
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 32)
                }
                .padding(.horizontal, 24)
                .padding(.top, 12)
            }
        }
        .task(id: model.introRequest) { await playIntro() }
        .sheet(isPresented: $showAddSheet) {
            AddDrinkSheet(todayMl: model.todayMl, goalMl: model.settings.goalMl,
                          lastHourMl: Hydration.lastHour(model.entries, now: .now)) { ml in
                model.addDrink(milliliters: ml)
                showAddSheet = false
            }
            .presentationDetents([.height(500)])
            .presentationDragIndicator(.visible)
        }
    }

    /// Counts the number up from 0 and raises the water with it, once per opening of the app.
    private func playIntro() async {
        let request = model.introRequest
        guard model.introPlayed != request else { return }
        intro = 0
        introRunning = true
        // On a cold start, wait briefly for the first load so it doesn't count up to 0
        for _ in 0..<20 where model.entries.isEmpty {
            try? await Task.sleep(for: .milliseconds(100))
        }
        guard !Task.isCancelled else { return }
        withAnimation(.timingCurve(0.33, 1, 0.68, 1, duration: 1.2)) { // ease-out cubic
            intro = 1
        } completion: {
            introRunning = false
            model.introPlayed = request
        }
    }

    private func greeting(_ now: Date) -> String {
        switch Calendar.current.component(.hour, from: now) {
        case 5...11: "Good morning"
        case 12...17: "Good afternoon"
        default: "Good evening"
        }
    }
}

/// Today's amount in large type. Animatable, so it counts through the values while it changes.
private struct HeroNumber: View, Animatable {
    var ml: Double
    let color: Color

    var animatableData: Double {
        get { ml }
        set { ml = newValue }
    }

    var body: some View {
        let unit = Text("L").font(.system(size: 38, weight: .semibold))
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            // Invisible twin of the unit on the left, so the digits themselves are centred
            unit.hidden()
            Text(Liters.value(Int(ml.rounded())))
                .font(.system(size: 128, weight: .semibold))
                .tracking(-5)
                .monospacedDigit()
            unit
        }
        .foregroundStyle(color)
        .lineLimit(1)
        .minimumScaleFactor(0.5)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Liters.text(Int(ml.rounded())))
    }
}

/// The streak as a small line under the number: a warm flame while it runs, grey at zero.
private struct StreakLine: View {
    let streak: Int

    var body: some View {
        HStack(spacing: 2) {
            Image(systemName: "flame.fill")
                .font(.system(size: 22))
                .foregroundStyle(streak > 0 ? Theme.flame : Theme.noData)
            Text("\(streak)")
                .font(.system(size: 24, weight: .bold))
                .monospacedDigit()
                .foregroundStyle(streak > 0 ? Theme.navy : Theme.muted)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(streak)-day streak")
    }
}

/// The bottle's connection and battery in one pill; tap to connect or disconnect.
private struct BottlePill: View {
    @EnvironmentObject private var model: AppModel

    private var text: String {
        switch model.bottle.link {
        case .connected: (["Paired"] + [model.bottle.batteryPercent.map { "\($0) %" }].compactMap { $0 }).joined(separator: " · ")
        case .connecting: "Pairing…"
        case .disconnected: "Bottle off"
        }
    }

    var body: some View {
        let connected = model.bottle.link == .connected
        Button { model.toggleBottle() } label: {
            HStack(spacing: 6) {
                Image(systemName: "waterbottle.fill")
                    .foregroundStyle(Theme.navy)
                    .opacity(connected ? 1 : 0.45)
                Text(text)
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.ink)
                Circle()
                    .fill(connected ? Theme.blue : Theme.noData)
                    .frame(width: 8, height: 8)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .glassSurface(in: Capsule(), interactive: true)
        }
        .buttonStyle(.pressable)
        .accessibilityLabel("\(model.bottle.name): \(text)")
        .accessibilityHint(model.bottle.link == .disconnected ? "Connects the bottle" : "Disconnects the bottle")
    }
}

/// Backend status as a cloud; tap to reload.
private struct CloudButton: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let offline = model.errorMessage != nil
        let (symbol, description) = offline
            ? ("icloud.slash.fill", "Backend offline, tap to retry")
            : model.isLive
                ? ("checkmark.icloud.fill", "Live connection to the backend")
                : ("arrow.clockwise.icloud.fill", "Connecting to the backend")
        Button { model.refresh() } label: {
            Image(systemName: symbol)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(offline ? Theme.red : Theme.blue)
                .contentTransition(.symbolEffect(.replace))
                .frame(width: 44, height: 44)
                .glassSurface(in: Circle(), interactive: true)
        }
        .buttonStyle(.pressable)
        .accessibilityLabel(description)
    }
}

/// The round plus that opens the add sheet.
private struct AddButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "plus")
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(Theme.blue)
                .frame(width: 72, height: 72)
                .glassSurface(in: Circle(), interactive: true, shadowRadius: 16)
        }
        .buttonStyle(.pressable)
        .accessibilityLabel("Add a drink")
    }
}

private struct AddDrinkSheet: View {
    let todayMl: Int
    let goalMl: Int
    let lastHourMl: Int
    let onAdd: (Int) -> Void
    @State private var custom = 200
    @State private var showInfo = false

    private let presets = [("Cup", 150), ("Glass", 250), ("Can", 330), ("Bottle", 500)]

    /// A gentle hint when the amount would be more than the body needs.
    private var warning: String? {
        if lastHourMl + custom > Hydration.maxPerHourMl {
            return "That's over 1 L within an hour. Better spread it out."
        }
        if Double(todayMl + custom) >= Double(goalMl) * Hydration.tooMuchFactor {
            return "That brings you to \(Liters.text(todayMl + custom)), well above your goal."
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
                            GlassIcon(level: min(Double(ml) / 600, 0.85))
                                .frame(width: 20, height: 24)
                                .padding(.bottom, 6)
                            Text(Liters.text(ml)).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.ink)
                            Text(name).font(.caption).foregroundStyle(Theme.muted)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Theme.sky, in: RoundedRectangle(cornerRadius: 18))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Theme.water, lineWidth: 1))
                    }
                    .buttonStyle(.pressable)
                    .accessibilityLabel("Add \(name), \(Liters.text(ml))")
                }
            }
            .padding(.top, 10)

            HStack(alignment: .firstTextBaseline) {
                Text("Other amount").font(.subheadline.weight(.semibold)).foregroundStyle(Theme.ink)
                Spacer()
                Text(Liters.text(custom))
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
            }
            .tint(Theme.blue)
            .accessibilityValue(Liters.text(custom))
            HStack {
                Text(Liters.text(50))
                Spacer()
                Text(Liters.text(1000))
            }
            .font(.caption)
            .foregroundStyle(Theme.muted)

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
                Text("Add \(Liters.text(custom))")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
            }
            .prominentCapsuleButton()
        }
        .padding(24)
        .animation(.default, value: custom)
    }
}

/// A plain outlined glass, filled up to `level` (0…1).
private struct GlassIcon: View {
    let level: Double

    var body: some View {
        Canvas { context, size in
            let inset: CGFloat = 1
            let top = inset
            let bottom = size.height - inset
            let taper = size.width * 0.14
            func left(_ y: CGFloat) -> CGFloat { inset + taper * (y - top) / (bottom - top) }
            func right(_ y: CGFloat) -> CGFloat { size.width - inset - taper * (y - top) / (bottom - top) }

            let waterTop = bottom - (bottom - top) * level
            var water = Path()
            water.move(to: CGPoint(x: left(waterTop), y: waterTop))
            water.addLine(to: CGPoint(x: right(waterTop), y: waterTop))
            water.addLine(to: CGPoint(x: right(bottom), y: bottom))
            water.addLine(to: CGPoint(x: left(bottom), y: bottom))
            water.closeSubpath()
            context.fill(water, with: .color(Theme.blue.opacity(0.3)))

            var glass = Path()
            glass.move(to: CGPoint(x: inset, y: top))
            glass.addLine(to: CGPoint(x: left(bottom), y: bottom))
            glass.addLine(to: CGPoint(x: right(bottom), y: bottom))
            glass.addLine(to: CGPoint(x: size.width - inset, y: top))
            context.stroke(glass, with: .color(Theme.blue), style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
        }
        .accessibilityHidden(true)
    }
}
