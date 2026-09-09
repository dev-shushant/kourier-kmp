# Kourier SDK: Best-in-Class Production Optimization Plan

This document outlines the detailed architecture and code improvement plan to make **Kourier KMP** a best-in-class in-app network inspection SDK for Kotlin Multiplatform (Android & iOS).

---

## Executive Summary

Kourier provides a comprehensive suite of in-app network inspection tools across Android, iOS, Ktor, OkHttp, Darwin/NSURLSession, and Compose Multiplatform. To bring the SDK from good to **industry-leading** (on par with or exceeding tools like Chucker, Pulse, and Proxyman), this plan targets 4 core areas:

1. **Interceptor Resilience**: Eliminate thread hangs on streams (SSE), capture network errors in Ktor, and ensure clean HTTP redirection in Darwin.
2. **Storage & Memory Efficiency**: Prevent full-body JSON deserialization of all transactions on every UI event.
3. **Touch & UI Isolation**: Ensure secondary overlay windows pass touches through to host apps transparently.
4. **Security & PII Masking**: Expand redaction to nested JSON objects and preserve URL fragments.

---

## Detailed Plan by Phase

### Phase 1: Interceptors & Runtime Safety

#### 1. Ktor 3.x Network Error Handling (`kourier-interceptor-ktor`)
- **Location**: `kourier-interceptor-ktor/src/commonMain/kotlin/dev/shushant/kourier/interceptor/ktor/KourierKtorPlugin.kt`
- **Issue**: Ktor client plugins hook `onRequest` and `onResponse`. If a network call fails due to timeouts, DNS failures, or SSL errors, `onResponse` is never invoked, leaving transactions permanently stuck in `TransactionStatus.PENDING`.
- **Implementation**:
  - Use `on(Send)` inside `createClientPlugin`.
  - Wrap `proceed(request)` in a `try / catch (cause: Throwable)` block.
  - On error, extract the initial transaction from request attributes, compute duration, construct an `ErrorPayload`, mark `TransactionStatus.FAILED`, and call `KourierCore.recordTransactionCompleted(failedTx)`.
  - Re-throw `cause` to preserve host application behavior.

#### 2. OkHttp SSE & Streaming Response Hang Prevention (`kourier-interceptor-okhttp`)
- **Location**: `kourier-interceptor-okhttp/src/main/kotlin/dev/shushant/kourier/interceptor/okhttp/KourierOkHttpInterceptor.kt`
- **Issue**: `source.request(maxBytes + 1)` blocks synchronously until `maxBytes + 1` bytes are received. For Server-Sent Events (`text/event-stream`), WebSockets (HTTP 101), or infinite chunked streams, this blocks the calling thread and freezes host app communication.
- **Implementation**:
  - Inspect `response.code` and `Content-Type` before buffering.
  - Detect `text/event-stream`, `multipart/x-mixed-replace`, and HTTP 101.
  - If detected, return a non-blocking descriptor: `[Streaming Response - Inspection Bypassed]` with `contentLength = 0L` and `isTruncated = false`.

#### 3. iOS Floating Overlay Touch Pass-Through (`kourier-ui`)
- **Location**: `kourier-ui/src/iosMain/kotlin/dev/shushant/kourier/ui/triggers/IosOverlayController.kt`
- **Issue**: The overlay `UIWindow` spans the full screen at `UIWindowLevelStatusBar - 1.0`. Standard `UIWindow` instances intercept all touches across their frame, causing the host app underneath to become unresponsive to taps.
- **Implementation**:
  - Subclass `UIWindow` as a passthrough window and override `hitTest(point, withEvent)`.
  - If the hit view is the window itself or the root view controller's container view, return `null` so UIKit forwards touches down the window hierarchy to the host app.

#### 4. Darwin Interceptor Hardening (`kourier-interceptor-darwin`)
- **Location**: `kourier-interceptor-darwin/src/iosMain/kotlin/dev/shushant/kourier/interceptor/darwin/KourierURLProtocol.kt`
- **Issue**:
  - `delegateQueue = NSOperationQueue.mainQueue` dispatches all chunked response data onto the main thread, causing frame drops during large downloads.
  - Missing HTTP 301/302 redirection handling breaks redirect chains.
  - `rawRequest.HTTPBodyStream` is ignored when `rawRequest.HTTPBody` is null (common in Alamofire and `URLSession.uploadTask`).
- **Implementation**:
  - Switch `delegateQueue` to a dedicated background `NSOperationQueue` or `null`.
  - Implement `URLSession(_:task:willPerformHTTPRedirection:newRequest:completionHandler:)` and forward via `client?.URLProtocol(this, wasRedirectedToRequest: newRequest, redirectResponse: response)`.
  - Safely stream or buffer `HTTPBodyStream` for request payloads.

---

### Phase 2: Performance & Storage Engine

#### 1. SQLDelight Lightweight Summary Entity (`kourier-storage`)
- **Location**:
  - `kourier-storage/src/commonMain/sqldelight/dev/shushant/kourier/storage/db/KourierDatabase.sq`
  - `kourier-storage/src/commonMain/kotlin/dev/shushant/kourier/storage/SQLiteKourierStorage.kt`
  - `kourier-ui/src/commonMain/kotlin/dev/shushant/kourier/ui/presentation/InspectorStateHolder.kt`
- **Issue**: `selectAll` queries `serializedData` (full request/response bodies, headers, call stacks) and runs `json.decodeFromString<HttpTransaction>` on every single transaction whenever any event occurs. With 200+ requests, this introduces massive GC pressure.
- **Implementation**:
  - Add query `selectSummaries`:
    ```sql
    selectSummaries:
    SELECT id, timestamp, method, url, host, path, statusCode, durationMs, isFailed
    FROM TransactionEntity
    ORDER BY timestamp DESC;
    ```
  - Create a lightweight `TransactionSummary` model for the list screen.
  - Only load full `serializedData` via `selectById` when a user navigates to `TransactionDetailScreen` or triggers an export.

#### 2. Throttled Database Pruning (`kourier-core`)
- **Location**: `kourier-core/src/commonMain/kotlin/dev/shushant/kourier/core/engine/CaptureRuntime.kt`
- **Issue**: `storage.trimToCount(...)` executes a `DELETE ... WHERE id NOT IN (SELECT ...)` subquery on every single request start.
- **Implementation**:
  - Throttle trimming using an atomic counter (`incrementAndGet() % 50 == 0`) or run it strictly on `Event.Maintain` and backgrounding.

---

### Phase 3: Security & Data Masking Hardening

#### 1. Recursive JSON Object & Array Redaction (`kourier-core`)
- **Location**: `kourier-core/src/commonMain/kotlin/dev/shushant/kourier/core/config/DataMasker.kt`
- **Issue**: Precompiled regexes only match string literals (`"key": "..."`) and primitives (`"key": 123`). Nested objects (e.g., `"credentials": { ... }`) or arrays (e.g., `"tokens": [ ... ]`) are left completely exposed.
- **Implementation**:
  - Enhance `DataMasker` to detect object `{ ... }` and array `[ ... ]` values for masked keys and replace the inner content with `"••••••••"`.

#### 2. URL Fragment Preservation (`kourier-core`)
- **Location**: `kourier-core/src/commonMain/kotlin/dev/shushant/kourier/core/config/DataMasker.kt`
- **Issue**: Splitting solely on `?` leaves `#fragment` attached to the final query parameter, corrupting the URL after redaction.
- **Implementation**:
  - Extract `#fragment` before parsing query parameters and re-attach it to the sanitized base URL and query string.

---

### Phase 4: Platform & Swift Polish

#### 1. SwiftUI iOS 17+ Modernization (`Sources/KourierSwift/Kourier.swift`)
- **Issue**: Single-argument `onChange(of: isPresented) { newValue in }` is deprecated in iOS 17+.
- **Implementation**: Update to dual-argument closure:
  ```swift
  .onChange(of: isPresented) { _, newValue in
      if newValue { Kourier.shared.showUI() } else { Kourier.shared.hideUI() }
  }
  ```

#### 2. Android 13+ Notification Permission Guard (`kourier-ui`)
- **Location**: `kourier-ui/src/androidMain/kotlin/dev/shushant/kourier/ui/triggers/NotificationTrigger.android.kt`
- **Issue**: On API 33+, calling `notificationManager.notify()` without runtime `POST_NOTIFICATIONS` causes silent drops or warnings.
- **Implementation**:
  - Guard with `NotificationManagerCompat.from(context).areNotificationsEnabled()`.

---

## Verification & Test Plan

1. **Ktor Interceptor Tests**:
   - Simulate network failure in `KourierKtorPluginTest.kt` and assert transaction transitions from `PENDING` to `FAILED` with error stack trace.
2. **OkHttp Interceptor Tests**:
   - Add test with `Content-Type: text/event-stream` and verify the call proceeds immediately without buffer exhaustion.
3. **Data Masking Tests**:
   - Add unit tests in `DataMaskerTest.kt` covering nested JSON objects (`"auth": { "key": "123" }`) and URLs with fragments (`https://example.com/api?token=secret#section`).
4. **Compilation & Packaging**:
   - Run `./gradlew :kourier-ios:compileKotlinIosSimulatorArm64`
   - Run `./gradlew :kourier-interceptor-darwin:compileKotlinIosSimulatorArm64`
   - Run `./gradlew :kourier-core:testDebugUnitTest`
