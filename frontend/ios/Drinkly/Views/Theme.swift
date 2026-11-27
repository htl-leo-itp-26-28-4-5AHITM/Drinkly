import SwiftUI
import UIKit

/// The app's colors, each with a light and a dark variant that follow the system appearance.
/// Values match the Android app.
enum Theme {
    static let blue = Color(light: 0x1F7AC6, dark: 0x3D91DA)
    static let deepBlue = Color(light: 0x135A9C, dark: 0x8CC4F0)
    /// Page background.
    static let sky = Color(light: 0xEFF6FC, dark: 0x0E1621)
    static let water = Color(light: 0xD3E6F5, dark: 0x1F3651)
    /// Strong blue for headings and marks; in dark mode a light blue so it stands out.
    static let navy = Color(light: 0x2B4775, dark: 0xB4CCEB)
    static let ink = Color(light: 0x1B2B3F, dark: 0xE6EDF5)
    /// Slightly blue grey, dark enough for small text on white (contrast about 5:1).
    static let muted = Color(light: 0x5D6F86, dark: 0x97A6B8)
    static let line = Color(light: 0xE2EAF2, dark: 0x253241)
    static let noData = Color(light: 0xC9D2DC, dark: 0x3A4654)
    static let red = Color(light: 0xD64545, dark: 0xF07272)
    /// Cards, sheets and pills on top of the page.
    static let card = Color(light: 0xFFFFFF, dark: 0x18222E)
    static let shadow = Color(light: 0x2B4775, dark: 0x000000)
    /// The streak's flame, the one warm accent.
    static let flame = Color(hex: 0xFF8A3D)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }

    /// A color that switches with the system appearance.
    init(light: UInt32, dark: UInt32) {
        self.init(uiColor: UIColor { traits in
            UIColor(Color(hex: traits.userInterfaceStyle == .dark ? dark : light))
        })
    }

    /// Mix of two colors, `t` = 0 gives `self`, 1 gives `other`.
    func mixed(with other: Color, by t: Double, in environment: EnvironmentValues) -> Color {
        let a = resolve(in: environment)
        let b = other.resolve(in: environment)
        let t = Float(min(max(t, 0), 1))
        return Color(red: Double(a.red + (b.red - a.red) * t),
                     green: Double(a.green + (b.green - a.green) * t),
                     blue: Double(a.blue + (b.blue - a.blue) * t))
    }
}

extension HydrationStatus {
    /// Main color for the status, used for the big number. In light mode the less you drank, the paler;
    /// on dark water it works the other way round: the more you drank, the brighter.
    var accent: Color {
        switch self {
        case .behind: Color(light: 0x4A9EDB, dark: 0x6E9FCB)
        case .slightlyBehind: Color(light: 0x2F8AD0, dark: 0x7FB5E4)
        case .onTrack: Color(light: 0x1F7AC6, dark: 0x94C8F2)
        case .goalReached: Color(light: 0x135A9C, dark: 0xB8DCFA)
        case .tooMuch: Color(light: 0x2B4775, dark: 0xD6EAFC)
        }
    }

    /// Color of the water surface on the Today screen; it gets deeper towards the bottom.
    var water: Color {
        switch self {
        case .behind: Color(light: 0xD4ECFA, dark: 0x132C46)
        case .slightlyBehind: Color(light: 0xC2E3F8, dark: 0x15324F)
        case .onTrack: Color(light: 0xADD7F4, dark: 0x173959)
        case .goalReached: Color(light: 0x9ACDF1, dark: 0x1A4064)
        case .tooMuch: Color(light: 0x8DC1EA, dark: 0x1D4770)
        }
    }

    var symbol: String {
        switch self {
        case .behind: "drop"
        case .slightlyBehind: "drop.halffull"
        case .onTrack: "drop.fill"
        case .goalReached: "checkmark.seal.fill"
        case .tooMuch: "hand.raised.fill"
        }
    }
}

// MARK: Liquid Glass

extension View {
    /// Liquid Glass on iOS 26, before that a solid card with a soft shadow like on Android.
    @ViewBuilder
    func glassSurface<S: Shape>(in shape: S, interactive: Bool = false, shadowRadius: CGFloat = 12) -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            glassEffect(interactive ? Glass.regular.interactive() : Glass.regular, in: shape)
        } else {
            cardSurface(in: shape, shadowRadius: shadowRadius)
        }
        #else
        cardSurface(in: shape, shadowRadius: shadowRadius)
        #endif
    }

    private func cardSurface<S: Shape>(in shape: S, shadowRadius: CGFloat) -> some View {
        background(Theme.card, in: shape)
            .shadow(color: Theme.shadow.opacity(0.18), radius: shadowRadius, y: 4)
    }
}
