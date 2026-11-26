import SwiftUI

/// The Today background: water rising from the sandy floor (empty = nothing drunk yet), a flower that
/// recovers as you drink, and a faint measuring scale on the right edge.
///
/// `fill` is today's intake divided by the goal; `topInset` and `bottomInset` keep the scale clear
/// of the title and the tab bar.
struct HydrationScene: View, Animatable {
    var fill: Double
    var goalMl: Int
    var water: Color
    var topInset: CGFloat
    var bottomInset: CGFloat
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
                let scale = SceneScale(size: size, topInset: topInset, bottomInset: bottomInset)

                context.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .linearGradient(Gradient(colors: [.white, Theme.sky]),
                                          startPoint: .zero, endPoint: CGPoint(x: 0, y: size.height))
                )

                let top = scale.y(share: min(fill, SceneScale.maxShare))
                context.fill(wave(size, top: top, phase: phase + .pi * 0.8, amplitude: 8), with: .color(water.opacity(0.55)))
                context.fill(wave(size, top: top, phase: phase, amplitude: 10), with: .color(water))

                drawSand(&context, size: size, scale: scale)
                drawFlower(&context, base: CGPoint(x: size.width * 0.16, y: scale.floor + 6),
                           height: min(130, scale.zero - scale.top) * 0.9, health: min(fill, 1))
                drawRuler(&context, size: size, scale: scale)
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

    private func drawSand(_ context: inout GraphicsContext, size: CGSize, scale: SceneScale) {
        var path = Path()
        path.move(to: CGPoint(x: 0, y: size.height))
        var x: CGFloat = 0
        while x <= size.width {
            let t = x / size.width * 2 * .pi
            path.addLine(to: CGPoint(x: x, y: scale.floor + 6 * sin(t * 1.3 + 0.6) + 3 * sin(t * 3.1)))
            x += 4
        }
        path.addLine(to: CGPoint(x: size.width, y: size.height))
        path.closeSubpath()
        context.fill(path, with: .linearGradient(
            Gradient(colors: [Theme.sand, Theme.sandShade]),
            startPoint: CGPoint(x: 0, y: scale.floor - 10), endPoint: CGPoint(x: 0, y: size.height)
        ))
        // a few pebbles
        for (fx, dy, r) in [(0.42, 14.0, 3.0), (0.47, 20.0, 2.0), (0.71, 12.0, 2.5), (0.88, 22.0, 3.5)] {
            let center = CGPoint(x: size.width * fx, y: scale.floor + dy)
            context.fill(Path(ellipseIn: CGRect(x: center.x - r * 1.4, y: center.y - r, width: r * 2.8, height: r * 2)),
                         with: .color(Theme.sandShade))
        }
    }

    /// Droops and fades to brown when you haven't drunk, stands up and blooms blue at the goal.
    private func drawFlower(_ context: inout GraphicsContext, base: CGPoint, height: CGFloat, health: Double) {
        let droop = 1 - health
        let length = height * (0.7 + 0.3 * health)
        let bend = CGPoint(x: base.x, y: base.y - length * 0.55)
        let angle = droop * 1.9 // the top of the stem tips over by up to ~110°
        let head = CGPoint(x: bend.x + sin(angle) * length * 0.45, y: bend.y - cos(angle) * length * 0.45)
        let green = Color(mix: 0xB0A16C, 0x4FA36B, health)

        var stem = Path()
        stem.move(to: base)
        stem.addQuadCurve(to: head, control: bend)
        context.stroke(stem, with: .color(green), style: StrokeStyle(lineWidth: 4, lineCap: .round))

        for side in [-1.0, 1.0] {
            var leaf = context
            leaf.translateBy(x: base.x, y: base.y - length * 0.28)
            leaf.rotate(by: .radians(side * (0.75 + droop * 1.6))) // up and out when healthy, hanging when wilted
            leaf.fill(Path(ellipseIn: CGRect(x: -5, y: -24, width: 10, height: 24)), with: .color(green))
        }

        let radius = 7 + 6 * health
        var bloom = context
        bloom.translateBy(x: head.x, y: head.y)
        bloom.rotate(by: .radians(angle))
        for i in 0..<6 {
            var petal = bloom
            petal.rotate(by: .degrees(Double(i) * 60))
            petal.fill(Path(ellipseIn: CGRect(x: -radius * 0.45, y: -radius * 1.6, width: radius * 0.9, height: radius * 1.2)),
                       with: .color(Color(mix: 0xB8A99A, 0x4A8FE0, health)))
        }
        bloom.fill(Path(ellipseIn: CGRect(x: -radius * 0.5, y: -radius * 0.5, width: radius, height: radius)),
                   with: .color(Color(mix: 0x9C7F55, 0xF4C24E, health)))
    }

    /// Faint ticks every 250 ml (500 ml for big goals), labelled every 500 ml (1 l), with the goal highlighted.
    private func drawRuler(_ context: inout GraphicsContext, size: CGSize, scale: SceneScale) {
        let x = size.width - 12
        let step = goalMl > 2500 ? 500 : 250
        let labelEvery = step * 2
        let maxMl = Int(Double(goalMl) * SceneScale.maxShare)
        let color = Theme.navy.opacity(0.28)

        var spine = Path()
        spine.move(to: CGPoint(x: x, y: scale.zero))
        spine.addLine(to: CGPoint(x: x, y: scale.y(share: SceneScale.maxShare)))
        context.stroke(spine, with: .color(color), lineWidth: 1)

        for ml in stride(from: step, through: maxMl, by: step) {
            let y = scale.y(share: Double(ml) / Double(goalMl))
            let major = ml % labelEvery == 0
            var tick = Path()
            tick.move(to: CGPoint(x: x - (major ? 12 : 6), y: y))
            tick.addLine(to: CGPoint(x: x, y: y))
            context.stroke(tick, with: .color(color), lineWidth: 1)
            if major && abs(ml - goalMl) >= step {
                context.draw(Text(Self.liters(ml)).font(.system(size: 10, weight: .medium)).foregroundStyle(Theme.muted.opacity(0.7)),
                             at: CGPoint(x: x - 16, y: y), anchor: .trailing)
            }
        }

        let goalY = scale.y(share: 1)
        var goalTick = Path()
        goalTick.move(to: CGPoint(x: x - 18, y: goalY))
        goalTick.addLine(to: CGPoint(x: x, y: goalY))
        context.stroke(goalTick, with: .color(Theme.blue.opacity(0.6)), lineWidth: 2)
        context.draw(Text("Goal \(Self.liters(goalMl))").font(.system(size: 10, weight: .semibold)).foregroundStyle(Theme.blue.opacity(0.8)),
                     at: CGPoint(x: x - 22, y: goalY), anchor: .trailing)
    }

    static func liters(_ ml: Int) -> String {
        let value = Double(ml) / 1000
        return value.formatted(.number.precision(.fractionLength(0...2))) + " l"
    }
}

/// Maps a share of the goal to a height: 0 sits on the sand, the goal at about 80 % of the free space.
private struct SceneScale {
    static let maxShare = 1.25

    let floor: CGFloat
    let zero: CGFloat
    let top: CGFloat

    init(size: CGSize, topInset: CGFloat, bottomInset: CGFloat) {
        floor = size.height - bottomInset - 30
        zero = floor + 22 // empty water (incl. its waves) hides behind the sand
        top = topInset + 80
    }

    func y(share: Double) -> CGFloat {
        zero - (zero - top) * 0.8 * share
    }
}
