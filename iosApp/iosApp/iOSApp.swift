import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()   // initialise Firebase a partir de GoogleService-Info.plist
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}