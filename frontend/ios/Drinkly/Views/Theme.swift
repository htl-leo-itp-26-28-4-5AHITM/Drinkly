import SwiftUI

enum Theme {
    static let blue = Color(hex: 0x1F7AC6)
    static let sky = Color(hex: 0xEFF6FC)
    static let water = Color(hex: 0xD3E6F5)
    static let navy = Color(hex: 0x2B4775)
    static let ink = Color(hex: 0x1B2B3F)
    static let muted = Color(hex: 0x7E8FA2)
    static let line = Color(hex: 0xE2EAF2)
    static let flame = Color(hex: 0xE8743B)
    static let warning = Color(hex: 0xD9572B)
    static let success = Color(hex: 0x169C86)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

extension HydrationStatus {
    /// Main color for the status, used for the big number and the chips.
    var accent: Color {
        switch self {
        case .goalReached: Theme.success
        case .onTrack: Theme.blue
        case .slightlyBehind: Color(hex: 0xC98A00)
        case .behind: Theme.warning
        }
    }

    /// Color of the water wave on the Today screen.
    var water: Color {
        switch self {
        case .goalReached: Color(hex: 0xCBEDE5)
        case .onTrack: Theme.water
        case .slightlyBehind: Color(hex: 0xF6E9C6)
        case .behind: Color(hex: 0xF8D9CC)
        }
    }
}
