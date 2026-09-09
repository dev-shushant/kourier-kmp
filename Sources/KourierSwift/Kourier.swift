#if canImport(KourierIos)
import KourierIos
import SwiftUI
import Combine

// MARK: - Native Swift Types

public struct NetworkStats: Equatable, Sendable {
    public let totalRequests: Int64
    public let activeRequests: Int32
    public let errorCount: Int64

    public init(totalRequests: Int64 = 0, activeRequests: Int32 = 0, errorCount: Int64 = 0) {
        self.totalRequests = totalRequests
        self.activeRequests = activeRequests
        self.errorCount = errorCount
    }
}

// MARK: - Config Builder Ergonomics

public extension KourierConfigBuilder {
    /// Masks HTTP request and response headers matching the given names (case-insensitive).
    @discardableResult
    func redactHeaders(_ headers: [String]) -> KourierConfigBuilder {
        let array = KotlinArray(size: Int32(headers.count)) { i in
            headers[Int(i.int32Value)] as NSString
        }
        self.redactHeaders(headers: array)
        return self
    }

    /// Masks JSON payload keys matching the given names recursively (case-insensitive).
    @discardableResult
    func redactPayloadKeys(_ keys: [String]) -> KourierConfigBuilder {
        let array = KotlinArray(size: Int32(keys.count)) { i in
            keys[Int(i.int32Value)] as NSString
        }
        self.redactPayloadKeys(keys: array)
        return self
    }

    /// Masks URL query parameters matching the given names (case-insensitive).
    @discardableResult
    func redactQueryParams(_ params: [String]) -> KourierConfigBuilder {
        let array = KotlinArray(size: Int32(params.count)) { i in
            params[Int(i.int32Value)] as NSString
        }
        self.redactQueryParams(params: array)
        return self
    }
}

// MARK: - Combine & Concurrency Support

public extension Kourier {
    /// A Combine publisher that emits the latest network telemetry statistics whenever traffic occurs.
    static var statsPublisher: AnyPublisher<NetworkStats, Never> {
        let subject = PassthroughSubject<NetworkStats, Never>()
        var cancelHandler: (() -> Void)?

        return subject
            .handleEvents(
                receiveSubscription: { _ in
                    cancelHandler = Kourier.shared.observeStats { total, active, errors in
                        subject.send(NetworkStats(
                            totalRequests: total.int64Value,
                            activeRequests: active.int32Value,
                            errorCount: errors.int64Value
                        ))
                    }
                },
                receiveCancel: {
                    cancelHandler?()
                }
            )
            .eraseToAnyPublisher()
    }
}

// MARK: - SwiftUI View Modifiers

public struct KourierInspectorModifier: ViewModifier {
    @Binding var isPresented: Bool

    public func body(content: Content) -> some View {
        if #available(iOS 17.0, macOS 14.0, *) {
            content
                .onChange(of: isPresented) { _, newValue in
                    handlePresentation(newValue)
                }
        } else {
            content
                .onChange(of: isPresented) { newValue in
                    handlePresentation(newValue)
                }
        }
    }

    private func handlePresentation(_ presented: Bool) {
        if presented {
            Kourier.shared.showUI()
        } else {
            Kourier.shared.hideUI()
        }
    }
}

public extension View {
    /// Binds the presentation of the Kourier network inspector to a boolean state.
    func kourierInspector(isPresented: Binding<Bool>) -> some View {
        self.modifier(KourierInspectorModifier(isPresented: isPresented))
    }
}
#endif
