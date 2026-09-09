import Foundation
import KourierIos

// MARK: - Network Stats (observable, mirrors KourierTelemetry.stats on Android)

final class NetworkStats: ObservableObject {
    @Published var total:      Int    = 0
    @Published var success:    Int    = 0
    @Published var errors:     Int    = 0
    @Published var avgLatency: String = "—"

    private var latencies: [Double] = []

    func update(statusCode: Int? = nil, duration: TimeInterval? = nil) {
        DispatchQueue.main.async {
            self.total += 1
            if let code = statusCode, code >= 200 && code < 400 {
                self.success += 1
            } else {
                self.errors += 1
            }
            if let d = duration {
                self.latencies.append(d * 1000)
                let avg = self.latencies.reduce(0, +) / Double(self.latencies.count)
                self.avgLatency = String(format: "%.0f", avg)
            }
        }
    }
}

// MARK: - API Client (mirrors ApiClient.kt)

final class ApiClient {
    static let shared = ApiClient()
    private init() {}

    /// Standard URLSession — intercepted globally via KourierURLSessionConfiguration.install()
    private lazy var session: URLSession = {
        let config = URLSessionConfiguration.default
        KourierURLSessionConfiguration.shared.enable(configuration: config)
        return URLSession(configuration: config)
    }()

    // MARK: 1. GET /get (200 OK)
    func fetchUsers(completion: @escaping (Int?, TimeInterval) -> Void) {
        guard let url = URL(string: "https://httpbin.org/get?query=kourier_sample&source=ios_sample") else { return }
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.httpMethod = "GET"
        req.setValue("KourierSample/1.0", forHTTPHeaderField: "User-Agent")
        req.setValue("application/json",  forHTTPHeaderField: "Accept")

        let start = Date()
        session.dataTask(with: req) { _, resp, _ in
            let code = (resp as? HTTPURLResponse)?.statusCode
            let duration = Date().timeIntervalSince(start)
            DispatchQueue.main.async { completion(code, duration) }
        }.resume()
    }

    // MARK: 2. POST /post with sensitive auth body
    func login(completion: @escaping (Int?, TimeInterval) -> Void) {
        guard let url = URL(string: "https://httpbin.org/post") else { return }
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.httpMethod = "POST"
        req.setValue("application/json",                               forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", forHTTPHeaderField: "Authorization")
        req.setValue("session_id=abcdef1234567890",                    forHTTPHeaderField: "Cookie")

        let body: [String: Any] = [
            "username":    "developer@example.com",
            "password":    "SuperSecretPassword99!",   // ← redacted by Kourier
            "token":       "tok_live_1234567890",       // ← redacted by Kourier
            "nested": [
                "credit_card": "4111-2222-3333-4444",  // ← redacted by Kourier
                "ssn":         "123-45-6789"           // ← redacted by Kourier
            ]
        ]
        req.httpBody = try? JSONSerialization.data(withJSONObject: body)

        let start = Date()
        session.dataTask(with: req) { _, resp, _ in
            let code = (resp as? HTTPURLResponse)?.statusCode
            let duration = Date().timeIntervalSince(start)
            DispatchQueue.main.async { completion(code, duration) }
        }.resume()
    }

    // MARK: 3. GET /status/404 (Client Error)
    func fetch404Error(completion: @escaping (Int?, TimeInterval) -> Void) {
        guard let url = URL(string: "https://httpbin.org/status/404") else { return }
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.httpMethod = "GET"

        let start = Date()
        session.dataTask(with: req) { _, resp, _ in
            let code = (resp as? HTTPURLResponse)?.statusCode
            let duration = Date().timeIntervalSince(start)
            DispatchQueue.main.async { completion(code, duration) }
        }.resume()
    }

    // MARK: 4. GET /status/500 (Server Error)
    func fetch500Error(completion: @escaping (Int?, TimeInterval) -> Void) {
        guard let url = URL(string: "https://httpbin.org/status/500") else { return }
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.httpMethod = "GET"

        let start = Date()
        session.dataTask(with: req) { _, resp, _ in
            let code = (resp as? HTTPURLResponse)?.statusCode
            let duration = Date().timeIntervalSince(start)
            DispatchQueue.main.async { completion(code, duration) }
        }.resume()
    }

    // MARK: 5. GET /delay/3 (Slow 3s call)
    func fetchSlowCall(completion: @escaping (Int?, TimeInterval) -> Void) {
        guard let url = URL(string: "https://httpbin.org/delay/3") else { return }
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.httpMethod = "GET"

        let start = Date()
        session.dataTask(with: req) { _, resp, _ in
            let code = (resp as? HTTPURLResponse)?.statusCode
            let duration = Date().timeIntervalSince(start)
            DispatchQueue.main.async { completion(code, duration) }
        }.resume()
    }

    // MARK: 6. Large Payload (>1MB)
    func fetchLargePayload(completion: @escaping (Int?, TimeInterval) -> Void) {
        guard let url = URL(string: "https://httpbin.org/bytes/1048576") else { return }
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.httpMethod = "GET"

        let start = Date()
        session.dataTask(with: req) { _, resp, _ in
            let code = (resp as? HTTPURLResponse)?.statusCode
            let duration = Date().timeIntervalSince(start)
            DispatchQueue.main.async { completion(code, duration) }
        }.resume()
    }

    // MARK: 7. Image Download
    func fetchImage(completion: @escaping (Int?, TimeInterval) -> Void) {
        guard let url = URL(string: "https://httpbin.org/image/png") else { return }
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.httpMethod = "GET"

        let start = Date()
        session.dataTask(with: req) { _, resp, _ in
            let code = (resp as? HTTPURLResponse)?.statusCode
            let duration = Date().timeIntervalSince(start)
            DispatchQueue.main.async { completion(code, duration) }
        }.resume()
    }
}
