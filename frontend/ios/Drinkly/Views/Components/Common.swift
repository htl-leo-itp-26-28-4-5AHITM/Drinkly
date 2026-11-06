import SwiftUI

struct ScreenTitle: View {
    let title: String
    var subtitle: String?

    init(_ title: String, subtitle: String? = nil) {
        self.title = title
        self.subtitle = subtitle
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(Theme.blue)
            if let subtitle {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(Theme.muted)
            }
        }
        .padding(.top, 12)
        .padding(.bottom, 8)
        .accessibilityAddTraits(.isHeader)
    }
}

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
        .background(.white, in: RoundedRectangle(cornerRadius: 22))
    }
}

struct Chip: View {
    let symbol: String
    let text: String
    let color: Color

    var body: some View {
        Label(text, systemImage: symbol)
            .font(.footnote.weight(.semibold))
            .foregroundStyle(color)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(color.opacity(0.12), in: Capsule())
    }
}

/// Round white icon next to a small label and a value, as in the bottle row on the Today screen.
struct InfoBadge: View {
    let symbol: String
    let label: String
    let value: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Theme.navy)
                .frame(width: 42, height: 42)
                .background(.white, in: Circle())
            VStack(alignment: .leading, spacing: 0) {
                Text(label).font(.caption).foregroundStyle(Theme.muted)
                Text(value).font(.callout.weight(.semibold)).foregroundStyle(Theme.ink)
            }
        }
        .accessibilityElement(children: .combine)
    }
}
