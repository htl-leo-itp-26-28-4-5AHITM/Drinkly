import Foundation

/// Amounts are shown in litres like on Android; the texts are English, so with a decimal point.
enum Liters {
    private static let format = FloatingPointFormatStyle<Double>.number
        .precision(.fractionLength(1...2))
        .locale(Locale(identifier: "en_US"))

    /// 1500 -> "1.5"
    static func value(_ ml: Int) -> String {
        (Double(ml) / 1000).formatted(format)
    }

    /// 1500 -> "1.5 L", 250 -> "0.25 L"
    static func text(_ ml: Int) -> String {
        value(ml) + " L"
    }
}
