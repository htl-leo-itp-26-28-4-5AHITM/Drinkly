import SwiftUI

enum Theme {
    static let blue = Color(hex: 0x1F7AC6)
    static let deepBlue = Color(hex: 0x135A9C)
    static let sky = Color(hex: 0xEFF6FC)
    static let water = Color(hex: 0xD3E6F5)
    static let navy = Color(hex: 0x2B4775)
    static let ink = Color(hex: 0x1B2B3F)
    // Dark enough for small text on white (contrast about 5:1)
    static let muted = Color(hex: 0x627387)
    static let line = Color(hex: 0xE2EAF2)
    static let noData = Color(hex: 0xC9D2DC)
    static let sand = Color(hex: 0xEFE2C4)
    static let sandShade = Color(hex: 0xE3D1A8)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }

    /// Mix of two hex colors, `t` = 0 gives `from`, 1 gives `to`.
    init(mix from: UInt32, _ to: UInt32, _ t: Double) {
        let t = min(max(t, 0), 1)
        func channel(_ shift: UInt32) -> Double {
            let a = Double((from >> shift) & 0xFF)
            let b = Double((to >> shift) & 0xFF)
            return (a + (b - a) * t) / 255
        }
        self.init(red: channel(16), green: channel(8), blue: channel(0))
    }
}

/// Everything is blue: the less you drank, the paler it gets.
extension HydrationStatus {
    /// Main color for the status, used for the big number and the status icon.
    var accent: Color {
        switch self {
        case .behind: Color(hex: 0x5AB0E6)
        case .slightlyBehind: Color(hex: 0x3796D6)
        case .onTrack: Theme.blue
        case .goalReached: Theme.deepBlue
        case .tooMuch: Theme.navy
        }
    }

    /// Color of the water on the Today screen.
    var water: Color {
        switch self {
        case .behind: Color(hex: 0xDDEFFA)
        case .slightlyBehind: Color(hex: 0xCFE6F7)
        case .onTrack: Color(hex: 0xBFDCF3)
        case .goalReached: Color(hex: 0xA6CDEE)
        case .tooMuch: Color(hex: 0x9CC2E6)
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
