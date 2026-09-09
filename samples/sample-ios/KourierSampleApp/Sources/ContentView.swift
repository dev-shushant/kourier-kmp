import SwiftUI
import KourierIos

// MARK: - Root View

struct ContentView: View {
    @StateObject private var stats = NetworkStats()

    var body: some View {
        ZStack {
            Color(hex: 0x121212).ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    // ── Header ──────────────────────────────────────────────────
                    VStack(spacing: 4) {
                        Text("Kourier Inspector Sample")
                            .font(.title2.bold())
                            .foregroundColor(.white)
                        Text("Shake phone or tap below to launch debugger")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    .padding(.top, 40)
                    .padding(.bottom, 20)

                    // ── Stats Card ────────────────────────────────────────────
                    StatsCard(stats: stats)
                        .padding(.horizontal, 20)

                    // ── Action Buttons ────────────────────────────────────────
                    VStack(spacing: 10) {
                        ActionButton(
                            title: "LAUNCH KOURIER DEBUGGER",
                            color: Color(hex: 0x238636)
                        ) {
                            Kourier.shared.showUI()
                        }
                        .padding(.vertical, 8)

                        ActionButton(
                            title: "1. GET 200 OK (GET /get)",
                            color: Color(hex: 0x58A6FF),
                            style: .outlined
                        ) {
                            ApiClient.shared.fetchUsers { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        ActionButton(
                            title: "2. POST Masked Auth (POST /post)",
                            color: Color(hex: 0xBC8CFF),
                            style: .outlined
                        ) {
                            ApiClient.shared.login { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        ActionButton(
                            title: "3. GET 404 Client Error",
                            color: Color(hex: 0xF85149),
                            style: .outlined
                        ) {
                            ApiClient.shared.fetch404Error { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        ActionButton(
                            title: "4. GET 500 Server Error",
                            color: Color(hex: 0xDA3633),
                            style: .outlined
                        ) {
                            ApiClient.shared.fetch500Error { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        ActionButton(
                            title: "5. Slow Call 3s Delay",
                            color: Color(hex: 0xD29922),
                            style: .outlined
                        ) {
                            ApiClient.shared.fetchSlowCall { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        ActionButton(
                            title: "6. Large Payload >1MB",
                            color: Color(hex: 0x3FB950),
                            style: .outlined
                        ) {
                            ApiClient.shared.fetchLargePayload { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        ActionButton(
                            title: "7. Image Download (PNG)",
                            color: Color(hex: 0x2EA043),
                            style: .outlined
                        ) {
                            ApiClient.shared.fetchImage { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 40)
            }
            .onAppear {
                if let mode = ProcessInfo.processInfo.environment["KOURIER_AUTO_MODE"] {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                        ApiClient.shared.fetchUsers { code, duration in stats.update(statusCode: code, duration: duration) }
                        ApiClient.shared.login { code, duration in stats.update(statusCode: code, duration: duration) }
                        ApiClient.shared.fetch404Error { code, duration in stats.update(statusCode: code, duration: duration) }
                        ApiClient.shared.fetch500Error { code, duration in stats.update(statusCode: code, duration: duration) }
                    }
                    if mode == "inspector" {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                            Kourier.shared.showUI()
                        }
                    } else if mode == "notification" || mode == "pill" {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                            Kourier.shared.switchToNotificationTray()
                        }
                    } else if mode == "bubble" {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                            Kourier.shared.switchToBubble()
                        }
                    } else if mode == "both" {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                            Kourier.shared.switchToBoth()
                        }
                    }
                }
            }
        }
    }
}
}

// MARK: - Stats Card

struct StatsCard: View {
    @ObservedObject var stats: NetworkStats

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Intercepted Requests")
                    .font(.caption.bold())
                    .foregroundColor(.gray)
                    .textCase(.uppercase)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 8)

            Divider().background(Color(hex: 0x30363D))

            HStack(spacing: 0) {
                StatItem(value: "\(stats.total)",   label: "TOTAL",   color: .white)
                Divider().background(Color(hex: 0x30363D)).frame(width: 1)
                StatItem(value: "\(stats.success)", label: "OK",      color: Color(hex: 0x3FB950))
                Divider().background(Color(hex: 0x30363D)).frame(width: 1)
                StatItem(value: "\(stats.errors)",  label: "ERRORS",  color: Color(hex: 0xF85149))
                Divider().background(Color(hex: 0x30363D)).frame(width: 1)
                StatItem(value: stats.avgLatency,   label: "AVG ms",  color: Color(hex: 0x58A6FF))
            }
            .frame(height: 60)
        }
        .background(Color(hex: 0x161B22))
        .cornerRadius(10)
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color(hex: 0x30363D), lineWidth: 1)
        )
    }
}

struct StatItem: View {
    let value: String
    let label: String
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.title3.bold())
                .foregroundColor(color)
            Text(label)
                .font(.system(size: 9, weight: .medium))
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Action Button

enum ButtonStyle { case filled, outlined }

struct ActionButton: View {
    let title: String
    let color: Color
    var style: ButtonStyle = .filled
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .frame(maxWidth: .infinity)
                .frame(height: 42)
                .foregroundColor(style == .filled ? .white : color)
                .background(style == .filled ? color : Color.clear)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(style == .filled ? Color.clear : color, lineWidth: 1)
                )
                .cornerRadius(8)
        }
    }
}

// MARK: - Helpers

extension Color {
    init(hex: UInt32) {
        self.init(
            red:   Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8)  & 0xFF) / 255,
            blue:  Double( hex        & 0xFF) / 255
        )
    }
}
