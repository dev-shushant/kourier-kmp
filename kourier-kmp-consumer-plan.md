# Kourier: One Artifact per Platform for Native Android, Native iOS and KMP Consumers

## Overview

**Goal.** Kourier must work cleanly in three kinds of host app, with no per-app workarounds. In each, the host adds **one dependency** that serves everything (capture, storage, UI, notifications and bubble):
1. Native Android apps: one Android artifact (AAR / Maven), plus the `kourier-noop` release stubs.
2. Native iOS apps: one prebuilt `KourierIos.xcframework` through Swift Package Manager (SPM).
3. Kotlin Multiplatform (KMP) apps, with or without Compose Multiplatform (CMP), such as eForme: one umbrella Maven coordinate that brings in everything.

**Decision (from review).** There is no UI-less / capture-only variant. Every distribution includes the UI. The earlier optional UI-less XCFramework sub-task has been removed.

**Problem (verified).**
- **iOS link failure in the KMP consumer.** eForme links `KourierIos.xcframework`, a static framework that bundles its own CMP runtime (it comes from `kourier-ui`). eForme's own `EformeShared.framework` also contains a CMP runtime. The link step fails with 114 duplicate symbols (`CMPView`, `CMPMetalLayer`, `CMPTextInputView`, `CMPOSLogger` and others).
- **Maven artifacts are unusable for iOS.** For version 0.0.3 on the `mvn-repo` branch, the `kourier-core`, `-storage`, `-ui` and `-interceptor-ktor` modules have only the root `.pom`.
  - The `-iosarm64` and `-iossimulatorarm64` artifacts return 404.
  - `kourier-interceptor-darwin` returns 404 entirely, because it is not in `publishableModules` in the root `build.gradle.kts`.
  - A KMP app that depends on `kourier-interceptor-ktor` therefore fails with "Could not find ...-iossimulatorarm64".
- **Version drift.** `version.properties` says 0.0.1, `libs.versions.toml` says `kourier-android = 0.0.9`, the root `Package.swift` points at `v0.0.1`, and tags go up to at least 0.0.3.
- **Docs.** The quickstarts use outdated API names and have no KMP-consumer guidance.

**Why one prebuilt framework cannot serve KMP hosts.** A prebuilt static framework that contains the UI must also contain a CMP runtime. A KMP host that already compiles CMP into its own framework will always clash with it at link time. So "single" means a different thing per audience:
- Native iOS: one binary (`KourierIos.xcframework`).
- KMP: one Maven coordinate (an umbrella module) that resolves to Kourier's klibs. Those are compiled into the host's own framework, so there is exactly one CMP runtime and one `KourierCore` singleton. The host never links `KourierIos.xcframework`.

**Chosen approach.**
- Add an umbrella KMP module (name to be decided, for example `kourier`) that exposes the whole SDK through `api()` dependencies on core, storage, ui, interceptor-ktor and interceptor-darwin, with one entry point. KMP hosts add one line.
- Keep `kourier-ios` as the native-iOS-only XCFramework, built from the same modules.
- Keep the capture modules (`kourier-core`, `kourier-storage`, interceptors) free of Compose. Only `kourier-ui` and `kourier-ios` use Compose, so the umbrella stays one coherent dependency.
- Single-runtime rule: capture and UI must live in the same Kotlin/Native binary, because `KourierCore` is an `object` singleton. Docs and a consumer sample make this rule explicit.

**Non-goals.**
- No UI-less / capture-only distribution.
- No `iosX64` target (project rule).
- No Ktor upgrade beyond 3.1.3 (a newer Ktor has a DEX-breaking field name).
- No per-app workarounds in eForme beyond migrating to the fixed library.

**Open item to confirm during review.** Whether `kourier-noop` should become KMP so KMP apps get release stubs for iOS too. It is verified in sub-task 4.

---

## Sub-Tasks

### 1. Verify the published artifact state and root-cause the missing iOS klibs
**Intent.** Confirm why iOS klibs and Gradle metadata are absent on the Maven repo before changing the pipeline. This avoids fixing the wrong thing. It needs a shell, so it runs in Agent mode.

**Expected Outcomes.**
- A written finding on whether the iOS publications were never produced (for example the publish job ran on a non-macOS host or skipped the iOS targets), or were produced and not pushed to `mvn-repo`.
- A list of exactly which variants and files exist for each module at 0.0.3.

**Todo List.**
1. Inspect the `.module` Gradle metadata files on the `mvn-repo` branch for each module at 0.0.3 and note which variants they declare.
2. List the real file tree per module on `mvn-repo` (klib, `.module`, sources) to see what was actually uploaded.
3. Locally run the publish task for the `DistributionRepo` and inspect the generated `build/repo` output for iOS publications.
4. Review the publish workflow logs, runner OS and Xcode version for the 0.0.3 release.
5. Record the findings at the bottom of this plan file for use by sub-task 2.

**Relevant Context.**
- `build.gradle.kts` (root): `publishableModules`, the `DistributionRepo` and GitHub Packages configuration, and the iOS exclusion rule for GitHub Packages.
- `settings.gradle.kts`: the `mvn-repo` resolution URL.
- `scripts/publish_optimized.sh` and `.github/workflows/publish.yml`.

**Status.** `[x] done`

**Findings (verified).**
- On `mvn-repo` at 0.0.3, the root `.module` files list `iosArm64` / `iosSimulatorArm64` variants through `available-at` pointers to `*-iosarm64` / `*-iossimulatorarm64`. Those modules do not exist (404), so Gradle resolution fails for KMP consumers.
- Root cause 1: the exclusion rule in the root `build.gradle.kts` matched every publish task whose name contains `Ios` or `Darwin`, not only the GitHub Packages ones. It also dropped the iOS klibs from `DistributionRepo`.
- Root cause 2: `kourier-interceptor-darwin` was not in `publishableModules`.
- Local `publishAllPublicationsToDistributionRepoRepository` with the fixed rule produces `.klib` and `.module` files for every iOS target of every KMP module.
- `kourier-noop` was already KMP (Android + iOS) but had no common-entry-point mirror.
- `version.properties` is now at 0.0.3. `kourier-android = 0.0.9` in `libs.versions.toml` is still unaligned (sub-task 7).

---

### 2. Fix publishing so every KMP module ships its iOS klibs and metadata
**Intent.** Make the Maven repo a complete source for KMP consumers: root module metadata plus the Android, `iosarm64` and `iossimulatorarm64` variants of every module that a KMP app needs, including the new umbrella module from sub-task 3.

**Expected Outcomes.**
- `kourier-interceptor-darwin` is published.
- For every published KMP module, `-iosarm64` and `-iossimulatorarm64` artifacts, with `.module` metadata that resolves from a KMP consumer.
- The publish pipeline runs on macOS and fails loudly if any expected iOS variant is missing.
- The GitHub Packages exclusion rule for iOS is reviewed. The `DistributionRepo` stays the source of truth for iOS klibs.

**Todo List.**
1. Based on sub-task 1, add `kourier-interceptor-darwin` to `publishableModules` and apply the same publishing setup as the other KMP modules.
2. Make sure the iOS targets of every published KMP module have publications, with sources and Gradle metadata.
3. Decide whether `kourier-ios` is published to Maven. It should be distributed only as an XCFramework, not as a klib that KMP consumers could pull by mistake. Make that choice explicit.
4. Add a post-publish verification step (script or workflow step) that checks each expected artifact URL for the version and fails the release if any is missing.
5. Re-publish a test version, then confirm resolution from a clean KMP consumer (see sub-task 6).

**Relevant Context.**
- Root `build.gradle.kts` (lines around 96-98 describe the iOS klibs going to `DistributionRepo`).
- `scripts/publish_optimized.sh`, `.github/workflows/publish.yml`.
- Module build files: `kourier-core`, `kourier-storage`, `kourier-ui`, `kourier-interceptor-ktor`, `kourier-interceptor-darwin`.

**Status.** `[-] code done, pending release + consumer verification`

**Done.** Exclusion rule narrowed to the GitHub Packages tasks. `kourier-interceptor-darwin` and `kourier` added to `publishableModules`. A `verify_published_artifacts` guard in `scripts/publish_optimized.sh` fails the release if any `.module` or iOS `.klib` is missing. Local publish verified: all 7 KMP modules have both iOS klibs.

**Remaining.** Cut a new release (needs the CI token) and resolve it from a clean consumer (sub-task 6 / 9). `kourier-ios` is not in `publishableModules`, so it is not published as a klib (XCFramework only).

---

### 3. Umbrella module, module boundaries and the KMP entry point
**Intent.** Give KMP hosts one dependency and one documented entry point to initialize Kourier, install capture and open the UI from shared code, without the Swift facade. Keep the module boundaries clean.

**Expected Outcomes.**
- A new umbrella KMP module (Android + `iosArm64` + `iosSimulatorArm64`) that re-exports core, storage, ui, interceptor-ktor and interceptor-darwin through `api()`. It is registered in `settings.gradle.kts` and in `publishableModules`.
- A written module contract: Compose is allowed only in `kourier-ui`, `kourier-ios` and the umbrella (through ui).
- A guard (Gradle check or dependency assertion) that fails the build if Compose appears in `kourier-core`, `kourier-storage`, `kourier-interceptor-ktor` or `kourier-interceptor-darwin`.
- A documented KMP entry point: initialize, show or hide UI and bubble, observe stats. It is callable from shared code (`commonMain`, or `iosMain` plus `androidMain` actuals) without `KourierIos`.
- Darwin capture is documented for KMP consumers (the Ktor Darwin engine and `KourierURLSessionConfiguration`), including the rule that the inner session must use an ephemeral configuration with empty `protocolClasses`.

**Todo List.**
1. Audit the existing public API in `kourier-ui` (`KourierUI.kt`, `KourierUI.android.kt`, `KourierUI.ios.kt`) and `kourier-ios/.../Kourier.kt` to see what a KMP app can already call.
2. Decide the minimal common surface and where it lives. Prefer reusing existing `KourierUI` and `KourierCore` APIs over adding new layers.
3. Create the umbrella module and check that `api()` dependencies expose what a consumer needs, with no duplicate or conflicting declarations.
4. Verify that `KourierKtorPlugin` and the Darwin interceptor record into the same `KourierCore` singleton when compiled into one binary. Write the finding down.
5. Add the Compose-free dependency guard.
6. Audit `@ObjCName` and other naming for conflicts when these klibs are compiled into a consumer's framework.

**Progress.** Umbrella module `:kourier` created (Android + both iOS targets); it compiles and links as a static iOS framework. It re-exports core, ktor interceptor, ui, okhttp, `kourier-android` and (iOS) darwin through `api()`. Common `expect object Kourier` offers `showUI`, `hideUI`, `observeStats`. Android adds `init(context, block)`. iOS adds `initialize`, `showBubble`, `hideBubble`.
- Step 4 (singleton): verified by reading. `KourierKtorPlugin` and `KourierURLProtocol` both call the same `KourierCore` object from `kourier-core`, so they share one singleton when compiled into one binary. Using the Ktor plugin and the Darwin NSURLProtocol on the same traffic would record requests twice, so docs must say to use one per client.
- Step 5 (guard): done. `composeFreeModules` check in the root `build.gradle.kts` fails configuration if a Compose plugin is applied to core, storage, interceptors. Verified it passes today and fires on a violation.
- Step 6 (`@ObjCName`): the only usage is in `kourier-ios` (`doInit`), which KMP consumers never link. No conflicts in the umbrella.
- Still open: a real consumer test (sub-task 6) that proves the umbrella resolves from Maven and links next to the consumer's own CMP.

**Relevant Context.**
- `kourier-ui/src/commonMain/.../KourierUI.kt` and the platform actuals (`KourierUI.android.kt`, `kourier-ui/src/iosMain/.../KourierUI.ios.kt`).
- `kourier-ui/src/iosMain/.../controller/KourierViewController.kt` and `triggers/IosOverlayController.kt` (follow the iOS overlay rules in AGENTS.md).
- `kourier-interceptor-ktor/src/commonMain/.../KourierKtorPlugin.kt`.
- `kourier-interceptor-darwin/src/iosMain/.../KourierURLSessionConfiguration.kt` and `KourierURLProtocol.kt`.
- `kourier-ios/build.gradle.kts` (the existing "single framework" setup that the umbrella mirrors for KMP).
- `AGENTS.md` (iOS module architecture, notification and overlay rules).

**Status.** `[ ] pending`

---

### 4. Release stubs for KMP consumers
**Intent.** Let a KMP consumer ship Release builds with no inspector code on both Android and iOS, using the same dependency shape as Debug.

**Expected Outcomes.**
- A decision on whether `kourier-noop` becomes a KMP module (Android plus iOS) that mirrors all public APIs, including the KMP entry point from sub-task 3.
- If yes, it is published with iOS variants and documented as the Release counterpart.

**Todo List.**
1. Inspect `kourier-noop` (its current targets and API surface) against the final public API.
2. If it is Android-only, convert it to KMP with the same iOS targets and publish it per sub-task 2.
3. Add API-parity checks (compile a consumer against both real and noop).
4. Document the debug/release dependency pattern for KMP (for example a debug-only source set or flavor-based wiring).

**Relevant Context.**
- `kourier-noop/`, `kourier-android/`.
- eForme enables the inspector only for DEV and QA flavours (`core/utils/.../AppConfig.kt`, `enableInspector`).

**Status.** `[-] in progress`

**Done.** `kourier-noop` was already KMP. Added a no-op `dev.shushant.kourier.Kourier` mirror (common expect, Android and iOS actuals) matching the umbrella API. Compiles for Android and both iOS targets. Remaining: API-parity check and the debug/release docs.

---

### 5. Keep `KourierIos.xcframework` correct for pure native iOS apps
**Intent.** Keep the single self-contained SPM binary for native iOS apps working, and make its limits explicit so nobody links it next to another CMP runtime.

**Expected Outcomes.**
- `kourier-ios` still builds `KourierIos.xcframework` as a static framework with the Swift-facing facade, and it includes the UI.
- The root `Package.swift`, release zip and checksum are generated for the correct version by the pipeline, and no stale `v0.0.1` remains.
- Docs state that `KourierIos.xcframework` is for native iOS apps only. A KMP app that embeds CMP must use the umbrella artifact instead.
- The sample iOS app still builds and runs.

**Todo List.**
1. Confirm `assembleKourierIosReleaseXCFramework` still works after the sub-task 2 and 3 changes.
2. Make the publish script write the correct tag, URL and checksum into `Package.swift`.
3. Re-test the iOS sample (needs `OTHER_LDFLAGS = -lsqlite3 -lc++` and `CADisableMinimumFrameDurationOnPhone = true`).
4. Add a clear compatibility note in the README and iOS quickstart.

**Relevant Context.**
- `kourier-ios/build.gradle.kts`, `kourier-ios/src/iosMain/.../Kourier.kt`.
- `Package.swift` (root), `Sources/KourierSwift/`, `samples/sample-ios/`.
- `scripts/publish_optimized.sh`.

**Status.** `[ ] pending`

---

### 6. Consumer integration sample and CI gate
**Intent.** Prove, on every change, that a KMP + CMP app can consume Kourier from Maven through the single umbrella dependency and link on iOS. Today only the SDK's own sample is built.

**Expected Outcomes.**
- A `sample-kmp` (Android + iOS) that consumes the umbrella artifact by published coordinates, not project dependencies, and embeds CMP in its own shared framework.
- A CI job that builds it for Android and for iOS simulator and runs the link step.
- The existing `sample-android` and `samples/sample-ios` remain as native-consumer checks.

**Todo List.**
1. Create the KMP consumer sample with Ktor, CMP and the single Kourier dependency from sub-task 3.
2. Wire a Ktor client with `KourierKtorPlugin` and a Darwin engine that uses Kourier capture on iOS.
3. Add a CI job on macOS that builds the sample for Android and iOS simulator, and fails on any duplicate-symbol or unresolved-dependency error.
4. Keep the Ktor version at 3.1.3.

**Relevant Context.**
- `settings.gradle.kts` (module list), `sample-android/`, `samples/sample-ios/`, `.github/workflows/`.
- Project rules in `AGENTS.md` for Ktor, `iosArm64 + iosSimulatorArm64` only, and the Kourier module architecture.

**Status.** `[ ] pending`

---

### 7. Align versions and the release process
**Intent.** Remove the drift between `version.properties`, `libs.versions.toml`, `Package.swift` and git tags so every release has one version everywhere.

**Expected Outcomes.**
- One source of truth for the version, used by Gradle publishing (umbrella included), the XCFramework release, `Package.swift` and the docs.
- `libs.versions.toml` no longer carries a separate `kourier-android` version that disagrees with `version.properties`.
- A short release checklist.

**Todo List.**
1. Decide which file is authoritative and update the others (or generate them).
2. Fix `kourier-android = 0.0.9` and `VERSION_NAME = 0.0.1` against the existing tags.
3. Update `Package.swift` generation to use the same version.
4. Add a CHANGELOG entry for the release that contains these fixes.

**Relevant Context.**
- `version.properties`, `gradle/libs.versions.toml`, `Package.swift`, `CHANGELOG.md`, `scripts/publish_optimized.sh`.

**Status.** `[ ] pending`

---

### 8. Documentation
**Intent.** Give each consumer type one correct, tested path with one dependency.

**Expected Outcomes.**
- A new KMP consumer guide: the single umbrella dependency, initialization, Ktor plugin and Darwin capture, showing the UI, Debug vs Release, and the single-runtime rule.
- Corrected iOS and Android quickstarts that use the current API names (`doInit`, `showUI`, `showBubble`, `observeStats` and so on).
- A troubleshooting section for the duplicate-symbol link error and for the `sqlite3`/`c++` linker flags.
- `README.md` and `AGENTS.md` updated to match, with a distribution table (Android, native iOS, KMP).

**Todo List.**
1. Write the KMP guide from the working sample in sub-task 6.
2. Fix `docs/quickstart-ios.md` and `docs/quickstart-android.md`.
3. Add the duplicate-symbols troubleshooting entry.
4. Update the README distribution table.

**Relevant Context.**
- `docs/quickstart-ios.md`, `docs/quickstart-android.md`, `docs/SDK_OPTIMIZATION_PLAN.md`, `README.md`, `AGENTS.md`.

**Status.** `[ ] pending`

---

### 9. Migrate eForme to the fixed library
**Intent.** Remove the temporary Swift bridge and `KourierIos` linkage from eForme and use the library the supported KMP way. This confirms the library fix.

**Expected Outcomes.**
- eForme resolves the new Kourier version from Maven for Android and iOS, through the single umbrella dependency.
- The `libs.kourier.ktor` dependency is replaced by the umbrella in `core/network` (the Ktor one was removed because the iOS klibs did not resolve).
- The iOS app no longer links `KourierIos.xcframework`, and the `core/network/native/InspectorBridge` Swift package is removed if nothing else needs it.
- `xcodebuild -scheme Eforme` (Debug, simulator) links with no duplicate symbols, and the inspector shows captured traffic on both platforms.
- Release builds contain no inspector code.

**Todo List.**
1. Bump Kourier to the new version in eForme's `gradle/libs.versions.toml` and add the umbrella alias.
2. Re-add the Kourier Ktor plugin on the shared Ktor client and the Darwin capture, replacing the Swift bridge calls in `IosChuckerDebugInspector`.
3. Remove the `KourierIos` SPM dependency and bridge package, and clean the Xcode project and `Package.resolved`.
4. Keep the Android `KourierInitializer` approach where it is still valid.
5. Build and run both platforms. Confirm that traffic appears in the Kourier UI.

**Relevant Context (eForme, `/Users/shushanttiwari/StudioProjects/e-for-me-Android`).**
- `core/network/build.gradle.kts`, `core/network/src/iosMain/.../port/IosChuckerDebugInspector.kt` and `di/PlatformNetworkModule.kt`.
- `core/network/src/androidMain/.../port/AndroidChuckerDebugInspector.kt` (`KourierInitializer`).
- `core/network/native/InspectorBridge/`, `iosApp/Eforme.xcodeproj`, `iosApp/Eforme/Info.plist`.
- `iosApp/KotlinMultiplatformLinkedPackage/subpackages/_core_network/Package.swift`, `gradle/libs.versions.toml`, `settings.gradle.kts`.
- Do not touch the unrelated uncommitted changes in `project.pbxproj` and `NetworkInspectorConfiguration.kt`.

**Status.** `[ ] pending`

---

## Suggested Order and Dependencies
1 -> 2 -> 3 -> (4, 5 in any order) -> 6 -> 7 -> 8 -> 9.
- Sub-task 3 (umbrella) must be published through sub-task 2 before sub-tasks 6 and 9 can consume it from Maven.
- Sub-task 7 must land before the final release that sub-task 9 consumes.

## Known Unverified Points
- Gradle `.module` metadata and the real file tree of `mvn-repo` for 0.0.3 were not inspected, because shell access was blocked in Plan mode (sub-task 1).
- Whether `kourier-noop` is KMP today was not checked (sub-task 4).
- The exact public API of `KourierUI.kt` and its platform actuals was not read in detail (sub-task 3).
- Whether an umbrella module's `api()` re-exports resolve cleanly for iOS klib consumers needs a test (sub-tasks 3 and 6).
- The `.github` workflows beyond `publish.yml`, `gradle.properties` and the `Sources/KourierSwift` contents were not reviewed.
