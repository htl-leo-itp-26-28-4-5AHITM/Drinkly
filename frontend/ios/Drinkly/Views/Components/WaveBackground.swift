import SwiftUI

/// Light sky with a gently moving water surface; `level` is the share of the height covered by water.
struct WaveBackground: View {
    var level: Double
    var water: Color
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { timeline in
            let seconds = timeline.date.timeIntervalSinceReferenceDate
            let phase = seconds.truncatingRemainder(dividingBy: 7) / 7 * 2 * .pi
            ZStack {
                LinearGradient(colors: [.white, Theme.sky], startPoint: .top, endPoint: .bottom)
                WaveShape(level: level, phase: phase + .pi * 0.8, amplitude: 8)
                    .fill(water.opacity(0.55))
                WaveShape(level: level, phase: phase, amplitude: 10)
                    .fill(water)
            }
        }
    }
}

struct WaveShape: Shape {
    var level: Double
    var phase: Double
    var amplitude: Double

    var animatableData: Double {
        get { level }
        set { level = newValue }
    }

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let top = rect.height * (1 - level)
        path.move(to: CGPoint(x: 0, y: rect.height))
        var x: CGFloat = 0
        while x <= rect.width {
            let y = top + amplitude * sin(x / rect.width * 2 * .pi + phase)
            path.addLine(to: CGPoint(x: x, y: y))
            x += 4
        }
        path.addLine(to: CGPoint(x: rect.width, y: top + amplitude * sin(2 * .pi + phase)))
        path.addLine(to: CGPoint(x: rect.width, y: rect.height))
        path.closeSubpath()
        return path
    }
}
