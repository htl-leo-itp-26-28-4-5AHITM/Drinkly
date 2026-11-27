import SwiftUI
import UserNotifications

@main
struct DrinklyApp: App {
    @StateObject private var model = AppModel()
    @Environment(\.scenePhase) private var scenePhase

    init() {
        UNUserNotificationCenter.current().delegate = NotificationDelegate.shared
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(model)
        }
        .onChange(of: scenePhase) { old, phase in
            if phase == .active { model.refresh() }
            // Back from the background: Today counts its number up again
            if old == .background && phase == .active { model.requestIntro() }
        }
    }
}
