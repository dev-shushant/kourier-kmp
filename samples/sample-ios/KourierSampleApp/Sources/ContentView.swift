import SwiftUI
import KourierIos

// MARK: - Root View

struct ContentView: View {
    @Environment(\.colorScheme) private var systemColorScheme
    @State private var overrideDark: Bool? = nil
    @StateObject private var stats = NetworkStats()

    private var isDark: Bool {
        overrideDark ?? (systemColorScheme == .dark)
    }

    // Dynamic Day/Night tokens
    private var bgColor: Color {
        isDark ? Color(hex: 0x0D1117) : Color(hex: 0xF6F8FA)
    }
    private var cardBg: Color {
        isDark ? Color(hex: 0x161B22) : Color.white
    }
    private var cardBorder: Color {
        isDark ? Color(hex: 0x30363D) : Color(hex: 0xD0D7DE)
    }
    private var textPrimary: Color {
        isDark ? Color(hex: 0xF0F6FC) : Color(hex: 0x1F2328)
    }
    private var textSecondary: Color {
        isDark ? Color(hex: 0x8B949E) : Color(hex: 0x656D76)
    }

    var body: some View {
        ZStack {
            bgColor.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    // ── Header with Theme Toggle ──────────────────────────────────
                    HStack(alignment: .center) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Kourier Inspector")
                                .font(.title2.bold())
                                .foregroundColor(textPrimary)
                            Text("Shake phone or tap below to launch")
                                .font(.caption)
                                .foregroundColor(textSecondary)
                        }

                        Spacer()

                        // Day / Night toggle pill button
                        Button(action: { overrideDark = !isDark }) {
                            HStack(spacing: 4) {
                                Text(isDark ? "☀️ Day" : "🌙 Night")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(textPrimary)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(cardBg)
                            .cornerRadius(16)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(cardBorder, lineWidth: 1)
                            )
                        }
                        .padding(.trailing, 64)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 36)
                    .padding(.bottom, 20)

                    // ── Stats Card ────────────────────────────────────────────
                    StatsCard(stats: stats, isDark: isDark, cardBg: cardBg, cardBorder: cardBorder, textPrimary: textPrimary, textSecondary: textSecondary)
                        .padding(.horizontal, 20)

                    // ── Action Buttons ────────────────────────────────────────
                    VStack(spacing: 12) {
                        ActionButton(
                            title: "LAUNCH KOURIER DEBUGGER",
                            color: isDark ? Color(hex: 0x238636) : Color(hex: 0x1A7F37),
                            isDark: isDark
                        ) {
                            Kourier.shared.showUI()
                        }
                        .padding(.top, 8)

                        // Batch Simulator Button (The 1-tap showcase action!)
                        ActionButton(
                            title: "⚡ FIRE DEMO TRAFFIC BATCH",
                            color: isDark ? Color(hex: 0x1F6FEB) : Color(hex: 0x0969DA),
                            isDark: isDark
                        ) {
                            ApiClient.shared.fetchUsers { code, duration in stats.update(statusCode: code, duration: duration) }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) {
                                ApiClient.shared.login { code, duration in stats.update(statusCode: code, duration: duration) }
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.36) {
                                ApiClient.shared.fetch404Error { code, duration in stats.update(statusCode: code, duration: duration) }
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.54) {
                                ApiClient.shared.fetch500Error { code, duration in stats.update(statusCode: code, duration: duration) }
                            }
                        }

                        Spacer().frame(height: 10)

                        // ── Section 1: Security & Data Masking ──────────────────
                        SectionHeader(
                            title: "🔒 DATA MASKING & SECURITY",
                            subtitle: "Auto-redacts passwords, tokens, cookies & PII",
                            textColor: textPrimary,
                            subColor: textSecondary
                        )

                        ActionButton(
                            title: "POST /auth/login (Redacts Passwords & PII)",
                            engineTag: "URLSession",
                            color: isDark ? Color(hex: 0xBC8CFF) : Color(hex: 0x8250DF),
                            style: .outlined,
                            isDark: isDark
                        ) {
                            ApiClient.shared.login { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        Spacer().frame(height: 10)

                        // ── Section 2: Multiplatform Network Traffic ────────────
                        SectionHeader(
                            title: "🌐 MULTIPLATFORM NETWORK ENGINES",
                            subtitle: "Unified interception across URLSession and Ktor 3",
                            textColor: textPrimary,
                            subColor: textSecondary
                        )

                        ActionButton(
                            title: "GET /users (200 OK Standard Request)",
                            engineTag: "URLSession",
                            color: isDark ? Color(hex: 0x58A6FF) : Color(hex: 0x0969DA),
                            style: .outlined,
                            isDark: isDark
                        ) {
                            ApiClient.shared.fetchUsers { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        ActionButton(
                            title: "GET /image.png (Binary Media Preview)",
                            engineTag: "URLSession",
                            color: isDark ? Color(hex: 0x2EA043) : Color(hex: 0x116329),
                            style: .outlined,
                            isDark: isDark
                        ) {
                            ApiClient.shared.fetchImage { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        Spacer().frame(height: 10)

                        // ── Section 3: Diagnostics & Error Handling ─────────────
                        SectionHeader(
                            title: "⚠️ ERROR DIAGNOSTICS & RESILIENCE",
                            subtitle: "Real-time error badges, alert trays & latency logs",
                            textColor: textPrimary,
                            subColor: textSecondary
                        )

                        ActionButton(
                            title: "GET /status/404 (Client Error)",
                            engineTag: "404",
                            color: isDark ? Color(hex: 0xF85149) : Color(hex: 0xCF222E),
                            style: .outlined,
                            isDark: isDark
                        ) {
                            ApiClient.shared.fetch404Error { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        ActionButton(
                            title: "GET /status/500 (Server Crash Alert)",
                            engineTag: "500",
                            color: isDark ? Color(hex: 0xDA3633) : Color(hex: 0xA40E26),
                            style: .outlined,
                            isDark: isDark
                        ) {
                            ApiClient.shared.fetch500Error { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        ActionButton(
                            title: "GET /delay/3s (Latency & Waterfall)",
                            engineTag: "3s",
                            color: isDark ? Color(hex: 0xD29922) : Color(hex: 0x9A6700),
                            style: .outlined,
                            isDark: isDark
                        ) {
                            ApiClient.shared.fetchSlowCall { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }

                        ActionButton(
                            title: "GET /bytes/1MB (Large Payload Truncation)",
                            engineTag: "1MB",
                            color: isDark ? Color(hex: 0x3FB950) : Color(hex: 0x1A7F37),
                            style: .outlined,
                            isDark: isDark
                        ) {
                            ApiClient.shared.fetchLargePayload { code, duration in
                                stats.update(statusCode: code, duration: duration)
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 40)
                }
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

// MARK: - Stats Card

struct StatsCard: View {
    @ObservedObject var stats: NetworkStats
    var isDark: Bool = true
    var cardBg: Color = Color(hex: 0x161B22)
    var cardBorder: Color = Color(hex: 0x30363D)
    var textPrimary: Color = Color(hex: 0xF0F6FC)
    var textSecondary: Color = Color(hex: 0x8B949E)

    private var dividerColor: Color {
        isDark ? Color(hex: 0x30363D) : Color(hex: 0xE2E8F0)
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Intercepted Requests")
                    .font(.caption.bold())
                    .foregroundColor(textSecondary)
                    .textCase(.uppercase)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 8)

            Divider().background(dividerColor)

            HStack(spacing: 0) {
                StatItem(value: "\(stats.total)",   label: "TOTAL",   color: textPrimary, labelColor: textSecondary)
                Divider().background(dividerColor).frame(width: 1)
                StatItem(value: "\(stats.success)", label: "OK",      color: isDark ? Color(hex: 0x3FB950) : Color(hex: 0x1A7F37), labelColor: textSecondary)
                Divider().background(dividerColor).frame(width: 1)
                StatItem(value: "\(stats.errors)",  label: "ERRORS",  color: isDark ? Color(hex: 0xF85149) : Color(hex: 0xCF222E), labelColor: textSecondary)
                Divider().background(dividerColor).frame(width: 1)
                StatItem(value: stats.avgLatency,   label: "AVG ms",  color: isDark ? Color(hex: 0x58A6FF) : Color(hex: 0x0969DA), labelColor: textSecondary)
            }
            .frame(height: 60)
        }
        .background(cardBg)
        .cornerRadius(10)
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(cardBorder, lineWidth: 1)
        )
        .shadow(color: isDark ? Color.clear : Color.black.opacity(0.04), radius: 3, x: 0, y: 1)
    }
}

struct StatItem: View {
    let value: String
    let label: String
    let color: Color
    var labelColor: Color = .gray

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.title3.bold())
                .foregroundColor(color)
            Text(label)
                .font(.system(size: 9, weight: .medium))
                .foregroundColor(labelColor)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Section Header

struct SectionHeader: View {
    let title: String
    let subtitle: String
    let textColor: Color
    let subColor: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(textColor)
            Text(subtitle)
                .font(.system(size: 11))
                .foregroundColor(subColor)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 2)
    }
}

// MARK: - Action Button

enum ButtonStyle { case filled, outlined }

struct ActionButton: View {
    let title: String
    var engineTag: String = ""
    let color: Color
    var style: ButtonStyle = .filled
    var isDark: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(title)
                    .font(.system(size: 13, weight: .semibold))
                    .lineLimit(1)
                    .truncationMode(.tail)
                Spacer()
                if !engineTag.isEmpty {
                    Text(engineTag)
                        .font(.system(size: 10, weight: .bold))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(color.opacity(isDark ? 0.25 : 0.18))
                        .cornerRadius(4)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .padding(.horizontal, 14)
            .foregroundColor(style == .filled ? .white : color)
            .background(
                style == .filled
                    ? color
                    : color.opacity(isDark ? 0.12 : 0.08)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(
                        style == .filled
                            ? Color.clear
                            : color.opacity(isDark ? 0.55 : 0.45),
                        lineWidth: 1
                    )
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

