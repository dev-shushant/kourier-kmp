# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Build Commands

```bash
# Full sample-android debug APK (validates entire SDK)
./gradlew :sample-android:assembleDebug

# Compile-check a single KMP module without building the sample
./gradlew :kourier-interceptor-ktor:compileKotlinAndroid
./gradlew :kourier-ui:compileKotlinAndroid

# Compile-check iOS KMP modules (requires no Xcode — only generates .klib)
./gradlew :kourier-ios:compileKotlinIosSimulatorArm64
./gradlew :kourier-interceptor-darwin:compileKotlinIosSimulatorArm64

# Build the KourierIos.xcframework (requires full Xcode.app, NOT just CLT)
./gradlew :kourier-ios:assembleKourierIosXCFramework

# Run all tests
./gradlew test

# Run tests in a specific module
./gradlew :kourier-interceptor-okhttp:test
./gradlew :kourier-core:testDebugUnitTest
```

## Library Versions (gradle/libs.versions.toml)

- **Kotlin**: 2.4.10 | **AGP**: 9.4.0 | **Compose Multiplatform**: 1.12.0
- **Ktor**: **3.1.3** (NOT 3.2.0 — has a DEX-breaking space in field name `use streaming syntax` that crashes Android dex below minSdk 34)
- **SQLDelight**: 2.3.2 | **Coroutines**: 1.11.0 | **atomicfu**: 0.29.0

## Ktor 3.x Breaking Changes vs 2.x

- `HttpReceivePipeline` is now `Pipeline<HttpResponse, Unit>` — subject is `HttpResponse` directly, NOT `HttpResponseContainer`. Context (`this.context`) is `Unit`, not `HttpClientCall`. Get the call via `response.call`.
- `Headers` (from `HttpMessage.headers`) is `StringValues` — use `flattenForEach { k, v -> }` (import `io.ktor.util.flattenForEach`).
- `HttpRequestBuilder.headers` is `HeadersBuilder` (a `StringValuesBuilder`) — use `.entries().forEach { (k, values) -> values.forEach {...} }` instead, since `flattenForEach` only targets `StringValues`.
- `OutgoingContent.contentLength` is not accessible in KMP `commonMain` — use `0L` as fallback for unknown content in the `else` branch.
- Our domain types `dev.shushant.kourier.core.model.HttpRequest` / `HttpResponse` clash with Ktor's own `io.ktor.client.request.HttpRequest` / `io.ktor.client.statement.HttpResponse`. Always use import aliases: `import dev.shushant.kourier.core.model.HttpRequest as KourierHttpRequest`.

## Smart Cast Rule (Kotlin / KMP)

Properties declared `val` in a **different module** cannot be smart-cast after a null check — the compiler rejects this even if the property is `val`. Always extract to a local `val` first:
```kotlin
// ❌ Fails across modules
if (transaction.response != null) { transaction.response.statusCode }
// ✅ Correct
val resp = transaction.response; if (resp != null) { resp.statusCode }
// OR use !! if the branch guarantees non-null
val code = transaction.response!!.statusCode
```
Affected files: `StatusBadge.kt`, `OverviewTab.kt`, `ResponseTab.kt`, `SQLiteKourierStorage.kt`.

## BoxWithConstraints Scope

`maxWidth` / `maxHeight` inside `BoxWithConstraints { }` require an explicit receiver when used in nested lambdas or when the compiler loses track of the implicit receiver. Use:
```kotlin
BoxWithConstraints {
    val constraints = this
    val isWide = constraints.maxWidth >= 720.dp
}
```

## Module Architecture

```
kourier-core        — KourierCore singleton, KourierConfig, DataMasker, EventBus, models
kourier-storage     — SQLiteKourierStorage (SQLDelight) + InMemoryKourierStorage
kourier-interceptor-okhttp  — Android-only OkHttp interceptor
kourier-interceptor-ktor    — KMP Ktor 3.x plugin (Android + iOS)
kourier-interceptor-darwin  — iOS NSURLProtocol (iosMain only)
kourier-ui          — Compose Multiplatform UI + triggers (shake, bubble, notification)
kourier-noop        — Zero-overhead release stubs, mirrors all public APIs
sample-android      — Validation app
```

- `KourierCore` is an `object` singleton. Call order: `initialize()` → interceptors call `recordRequestStarted` → (optionally) `recordTransactionUpdated` → `recordTransactionCompleted`.
- `sample-android` must declare `implementation(project(":kourier-core"))` explicitly — transitive resolution from `kourier-ui` is not sufficient for `compileDebugKotlin`.
- `KourierActivity` is declared in `kourier-ui/src/androidMain/AndroidManifest.xml` and merged into the sample app — no manual manifest entry needed.
- SQLDelight generates `KourierDatabase` from `kourier-storage/src/commonMain/sqldelight/KourierDatabase.sq` at build time.

## iOS Module Architecture

```
kourier-ios              — iOS-only KMP module; ONLY framework the host app imports
  └── api: kourier-interceptor-darwin   — re-exported into KourierIos.xcframework
  └── implementation: kourier-core, kourier-storage, kourier-ui  (hidden from host)
```

- `kourier-ios/build.gradle.kts` uses `XCFramework("KourierIos")` + `xcf.add(this)` inside each `iosTarget.binaries.framework {}` block to register the assemble task.
- Kotlin vararg/array parameters bridge to Swift as `KotlinArray<NSString>`, NOT `[String]`. The sample uses helper `func kotlinArray(_ strings: String...) -> KotlinArray<NSString>` in `KourierSampleApp.swift`.
- The sample Xcode project (`samples/sample-ios/KourierSampleApp.xcodeproj`) requires `OTHER_LDFLAGS = -lsqlite3 -lc++` because SQLDelight is statically linked into `KourierIos.xcframework` and the host app must provide the system sqlite3 and libc++ symbols.
- XCFramework deployment target must match the SDK it was compiled against (`18.5` with Xcode 27 beta) — mismatches cause linker failures under Xcode 27 beta.
- `KourierIos.xcframework` assembly task is named `assembleKourierIosXCFramework` (derived from the `XCFramework` constructor name).
- `Kourier.initialize()` is annotated `@ObjCName("doInit")` (and requires `@OptIn(ExperimentalObjCName::class)` on the enclosing `object`) because `init` is a reserved keyword in Swift.
- `NSURLProtocol.registerClass(KourierURLProtocol)` passes the **companion object** directly — it extends `NSURLProtocolMeta` which IS an `ObjCClass`. Do NOT use `objcClassOf`/`getObjCClass` (both internal).
- `NSURLCacheStorageNotAllowed` cannot be imported as a standalone symbol — use `2L as NSURLCacheStoragePolicy` with `@Suppress("UNCHECKED_CAST")`.
- `allHTTPHeaderFields` on `NSURLRequest` requires its own explicit import: `import platform.Foundation.allHTTPHeaderFields` (it's an ObjC category extension, not in the base class).
- `useContents { x/y/z }` on `CMAcceleration` requires `import kotlinx.cinterop.useContents` explicitly.
- `String.format("%.1f", ...)` does NOT exist in KMP `commonMain` — use manual integer arithmetic (divide by 100, take remainder).
- iOS target set is `iosArm64 + iosSimulatorArm64` only — do NOT add `iosX64()`.
- The iOS sample app (`samples/sample-ios/KourierSampleApp/Sources/KourierSampleApp.swift`) is SwiftUI: `@main struct KourierSampleApp: App`, `ContentView: View`, shake via `UIViewRepresentable`.
- **`NSURLProtocol` inner session MUST use `ephemeralSessionConfiguration` + `protocolClasses = emptyList<Any>()`** — `defaultSessionConfiguration` causes `KourierURLProtocol` to re-intercept its own forwarded request (infinite loop → all transactions stuck PENDING). The `KOURIER_HANDLED_KEY` property tag alone does not prevent this.
- **`CADisableMinimumFrameDurationOnPhone = true` must be present in `Info.plist`** for any iOS app embedding Compose Multiplatform 1.12.0+. Missing it causes a fatal crash (`PlistSanityCheck`) on first Compose render. `UISceneConfigurations` does NOT fix it.
- **iOS Secondary Overlay Window Rules (`IosOverlayController`)**:
  - Any UIKit window mutation (`install()`, `uninstall()`, frame updates) MUST run on the main thread: guard with `if (!NSThread.isMainThread) { NSOperationQueue.mainQueue.addOperationWithBlock { ... }; return }`.
  - Window level must be `UIWindowLevelStatusBar - 1.0` (999.0) — `UIWindowLevelNormal + 1.0` is occluded beneath host app sheets, navigation bars, and map views.
  - Use `window.makeKeyAndVisible()` rather than `window.hidden = false` alone so the secondary window is attached to UIKit's rendering pipeline.
  - SwiftUI helpers `FloatingBubbleOverlay` and `NetworkStats` are sample-only helpers (not exported from `KourierIos.xcframework`); host apps rely on `IosOverlayController` or subscribe to `Kourier.shared.observeStats()`.

- **Persistent Notification Trigger (`NotificationTrigger.android.kt`)**:
  - Never re-create `NotificationChannel` on stats update emissions; guard channel creation with `@Volatile channelCreated` to initialize exactly once per process. Recreating the channel causes Android to silently drop subsequent `notify()` UI updates.
  - Always set `.setOnlyAlertOnce(true)` on `NotificationCompat.Builder` for ongoing low-importance notifications to avoid redraw suppression.
  - Keep notification update collection strictly within the background `CoroutineScope` in `startTriggers()` — do NOT trigger notification updates from Compose UI lifecycle/`LaunchedEffect`.
  - **Two-Channel Architecture**:
    - `CHANNEL_TRAY_ID = "kourier_traffic_tray_v4"` (`NOTIFICATION_TRAY_ID = 404101`): `IMPORTANCE_LOW`, `PRIORITY_LOW`, `ongoing=true`, `onlyAlertOnce=true`, `CATEGORY_STATUS`. Must stay docked quietly in the notification drawer under "Silent". NEVER pop up heads-up over the app during normal requests.
    - `CHANNEL_ALERT_ID = "kourier_error_alerts_v4"` (`NOTIFICATION_ERROR_ID = 404102`): `IMPORTANCE_HIGH`, `PRIORITY_HIGH`, `autoCancel=true`, `CATEGORY_ERROR`. Pops up heads-up ONLY when `hasNewError` occurs (`telemetry.errorCount > lastErrorCount` or 4xx/5xx).
  - **Overlay Occlusion**: `AndroidOverlayController` must detach the floating bubble when `KourierActivity` is resumed to ensure top bar action buttons are unobstructed.

## iOS Notification & Overlay Rules

- **Two-Tier Notification Presentation (`NotificationTrigger.ios.kt`)**:
  - `NOTIFICATION_TRAY_ID = "kourier_live_telemetry"`: In `KourierNotificationDelegate.userNotificationCenter`, foreground presentations return `UNNotificationPresentationOptionList` ONLY so standard requests (2xx, 3xx, data bytes) update silently in the Notification Center without intrusive heads-up banners.
  - `NOTIFICATION_ERROR_ID = "kourier_error_alert"`: Fires strictly when `telemetry.errorCount > lastErrorCount`. Returns `UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or UNNotificationPresentationOptionSound` to drop down a high-priority heads-up banner with alert sound and `UINotificationFeedbackTypeError` haptic.
- **Overlay Window Management (`IosOverlayController.kt`)**:
  - Automatically call `IosOverlayController.setVisible(false)` when `KourierViewController` is presented, and `setVisible(true)` when dismissed. This guarantees the top navigation bar (`[Share]`, `[Clear]`, `[Theme]`, `[Settings]`, `[X]`) is never occluded.

## Code Style

- KMP modules: `commonMain` / `androidMain` / `iosMain` source sets.
- `expect`/`actual` for platform utilities: `PlatformUtils` in `kourier-core/src/*/platform/`.
- All `suspend` functions that call SQLDelight use `withContext(Dispatchers.IO) { ...; Unit }` — the explicit `Unit` is required because SQLDelight query methods return `QueryResult<Long>`, not `Unit`.
- `DataMasker` and `KourierConfig.Builder` are in `dev.shushant.kourier.core.config`.

