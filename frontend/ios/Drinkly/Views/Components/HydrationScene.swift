import SwiftUI

/// The Today background: water rising from the bottom edge (empty = nothing drunk yet) to just below
/// the status bar once the goal is reached, so the wave stays visible.
///
/// `fill` is today's intake divided by the goal; `topInset` is the status bar height.
struct HydrationScene: View, Animatable {
    var fill: Double
    var water: Color
    var topInset: CGFloat
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var animatableData: Double {
        get { fill }
        set { fill = newValue }
    }

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { timeline in
            let seconds = timeline.date.timeIntervalSinceReferenceDate
            let phase = seconds.truncatingRemainder(dividingBy: 7) / 7 * 2 * .pi
            Canvas { context, size in
                context.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .linearGradient(Gradient(colors: [Theme.card, Theme.sky]),
                                          startPoint: .zero, endPoint: CGPoint(x: 0, y: size.height))
                )

                let zero = size.height + 12 // empty water hides below the screen
                let full = topInset + 24 // at the goal the wave still shows just below the status bar
                let top = zero - (zero - full) * min(max(fill, 0), 1)

                context.fill(wave(size, top: top, phase: phase + .pi * 0.8, amplitude: 8), with: .color(water.opacity(0.55)))
                // Clear at the surface, a little deeper towards the bottom
                let deep = water.mixed(with: Theme.blue, by: 0.22, in: context.environment)
                context.fill(
                    wave(size, top: top, phase: phase, amplitude: 10),
                    with: .linearGradient(Gradient(colors: [water, deep]),
                                          startPoint: CGPoint(x: 0, y: top), endPoint: CGPoint(x: 0, y: size.height))
                )
            }
        }
        .accessibilityHidden(true)
    }

    private func wave(_ size: CGSize, top: CGFloat, phase: Double, amplitude: CGFloat) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: 0, y: size.height))
        var x: CGFloat = 0
        while x <= size.width {
            path.addLine(to: CGPoint(x: x, y: top + amplitude * sin(x / size.width * 2 * .pi + phase)))
            x += 4
        }
        path.addLine(to: CGPoint(x: size.width, y: top + amplitude * sin(2 * .pi + phase)))
        path.addLine(to: CGPoint(x: size.width, y: size.height))
        path.closeSubpath()
        return path
    }
}
