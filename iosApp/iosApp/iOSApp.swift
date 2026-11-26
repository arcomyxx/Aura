import SwiftUI
import FirebaseCore
import ComposeApp

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        MainViewControllerKt.configureRevenueCat()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}