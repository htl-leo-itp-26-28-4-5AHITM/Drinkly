import SwiftUI

/// A simple drawing of the Drinkly smart bottle.
struct BottleIllustration: View {
    var dimmed = false

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            let capHeight = h * 0.16
            ZStack(alignment: .top) {
                RoundedRectangle(cornerRadius: w * 0.28)
                    .fill(LinearGradient(
                        colors: [Color(hex: 0x3B5D93), Theme.navy, Color(hex: 0x203A63)],
                        startPoint: .leading, endPoint: .trailing
                    ))
                    .frame(width: w, height: h - capHeight * 0.9)
                    .offset(y: capHeight * 0.9)
                RoundedRectangle(cornerRadius: w * 0.12)
                    .fill(Color(hex: 0x1E355C))
                    .frame(width: w * 0.62, height: capHeight)
                RoundedRectangle(cornerRadius: w * 0.06)
                    .fill(.white.opacity(0.18))
                    .frame(width: w * 0.12, height: h * 0.62)
                    .offset(x: -w * 0.26, y: capHeight + h * 0.06)
                Rectangle()
                    .fill(Color(hex: 0x8EC3EC).opacity(0.7))
                    .frame(width: w, height: h * 0.025)
                    .offset(y: h * 0.78)
            }
            .frame(width: w, height: h, alignment: .top)
        }
        .opacity(dimmed ? 0.45 : 1)
        .accessibilityHidden(true)
    }
}
