import SwiftUI
import KourierIos

@main
struct KourierSampleApp: App {

    init() {
        // Initialize Kourier once at app startup — mirrors SampleApplication.kt
        Kourier.shared.doInit { builder in
            builder.maxPayloadSize(bytes: 500 * 1024)   // 500 KB limit
            builder.maxRetentionCount(count: 1000)
            builder.retentionPeriodDays(days: 3)

            // Mask sensitive headers
            builder.redactHeaders(headers: kotlinArray("Authorization", "X-Api-Key", "Cookie", "Set-Cookie"))

            // Mask sensitive payload keys
            builder.redactPayloadKeys(keys: kotlinArray("password", "token", "secret", "credit_card", "ssn"))

            // Configure trigger style: .both (Floating Bubble + Notification Tray)
            builder.triggerStyle(style: .both)
            builder.captureCallStack(enable: true, maxDepth: 15)
        }

        // Register KourierURLProtocol globally so every URLSession is intercepted.
        // This is the only integration step needed beyond doInit() — the SDK owns
        // bubble and shake triggers internally (started inside doInit).
        KourierURLSessionConfiguration.shared.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    let host = url.host ?? url.path.replacingOccurrences(of: "/", with: "")
                    switch host {
                    case "show":
                        Kourier.shared.showUI()
                    case "hide":
                        Kourier.shared.hideUI()
                    case "bubble":
                        Kourier.shared.switchToBubble()
                    case "pill", "notification":
                        Kourier.shared.switchToNotificationTray()
                    case "both":
                        Kourier.shared.switchToBoth()
                    case "req-all":
                        ApiClient.shared.fetchUsers { _, _ in }
                        ApiClient.shared.login { _, _ in }
                        ApiClient.shared.fetch404Error { _, _ in }
                        ApiClient.shared.fetch500Error { _, _ in }
                    case "req-get":
                        ApiClient.shared.fetchUsers { _, _ in }
                    case "req-login":
                        ApiClient.shared.login { _, _ in }
                    case "req-404":
                        ApiClient.shared.fetch404Error { _, _ in }
                    case "req-500":
                        ApiClient.shared.fetch500Error { _, _ in }
                    default:
                        break
                    }
                }
        }
    }
}

// MARK: - KMP interop helper
//
// Kotlin vararg parameters are exposed to Swift as KotlinArray<NSString>, not [String].
// This factory converts a Swift variadic into the correct KMP type.

func kotlinArray(_ strings: String...) -> KotlinArray<NSString> {
    KotlinArray(size: Int32(strings.count)) { i in strings[Int(i.int32Value)] as NSString }
}
