import Foundation

enum BottleLink {
    case disconnected, connecting, connected
}

struct BottleState {
    var name = "Drinkly Bottle"
    var link = BottleLink.disconnected
    var batteryPercent: Int?
    var lastSync: Date?
}

/// The smart bottle, reached over Bluetooth. The app forwards every sip it reports
/// to the backend, so the bottle itself only ever talks to the phone.
///
/// `SimulatedBottle` stands in until the bottle's Bluetooth GATT profile is defined;
/// a CoreBluetooth implementation only has to provide these members.
@MainActor
protocol BottleConnection: AnyObject {
    var state: BottleState { get }
    var onStateChange: ((BottleState) -> Void)? { get set }
    /// Called with the amount in ml the bottle measured.
    var onSip: ((Int) -> Void)? { get set }

    func connect()
    func disconnect()
}

/// Demo bottle: pairs after a short delay, slowly drains its battery and sends sips on request.
@MainActor
final class SimulatedBottle: BottleConnection {
    private(set) var state = BottleState() {
        didSet { onStateChange?(state) }
    }
    var onStateChange: ((BottleState) -> Void)?
    var onSip: ((Int) -> Void)?

    private var task: Task<Void, Never>?

    func connect() {
        guard task == nil else { return }
        task = Task { [weak self] in
            self?.state.link = .connecting
            try? await Task.sleep(for: .seconds(1.2))
            guard let self, !Task.isCancelled else { return }
            state.link = .connected
            state.batteryPercent = state.batteryPercent ?? 85
            state.lastSync = .now
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(10 * 60))
                if Task.isCancelled { break }
                state.batteryPercent = max((state.batteryPercent ?? 85) - 1, 5)
            }
        }
    }

    func disconnect() {
        task?.cancel()
        task = nil
        state.link = .disconnected
    }

    func simulateSip(milliliters: Int) {
        guard state.link == .connected else { return }
        state.lastSync = .now
        onSip?(milliliters)
    }
}
