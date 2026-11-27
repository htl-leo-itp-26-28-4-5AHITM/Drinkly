import SwiftUI

struct Card<Content: View>: View {
    var title: String?
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let title {
                Text(title)
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.muted)
            }
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(Theme.card, in: RoundedRectangle(cornerRadius: 22))
    }
}

/// Shrinks and dims slightly while pressed, so every tap gets visible feedback.
struct PressableButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .opacity(configuration.isPressed ? 0.85 : 1)
            .animation(.spring(duration: 0.25), value: configuration.isPressed)
    }
}

extension ButtonStyle where Self == PressableButtonStyle {
    static var pressable: PressableButtonStyle { PressableButtonStyle() }
}

extension View {
    /// The main button of a screen or sheet: Liquid Glass (prominent) on iOS 26, a filled capsule before.
    @ViewBuilder
    func prominentCapsuleButton(size: ControlSize = .large) -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            buttonStyle(.glassProminent).tint(Theme.blue).controlSize(size)
        } else {
            buttonStyle(.borderedProminent).buttonBorderShape(.capsule).tint(Theme.blue).controlSize(size)
        }
        #else
        buttonStyle(.borderedProminent).buttonBorderShape(.capsule).tint(Theme.blue).controlSize(size)
        #endif
    }

    /// A secondary button: Liquid Glass on iOS 26, a bordered capsule before.
    @ViewBuilder
    func secondaryCapsuleButton() -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            buttonStyle(.glass).tint(Theme.blue)
        } else {
            buttonStyle(.bordered).buttonBorderShape(.capsule).tint(Theme.blue)
        }
        #else
        buttonStyle(.bordered).buttonBorderShape(.capsule).tint(Theme.blue)
        #endif
    }
}
