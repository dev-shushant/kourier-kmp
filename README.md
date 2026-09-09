# Kourier — SDK source

Kourier is an on-device HTTP inspection and telemetry SDK for Android and iOS, built with Kotlin Multiplatform and Compose Multiplatform. It captures traffic through OkHttp, Ktor, and URLSession integrations and provides an inspector with request details, payload redaction, local storage, and export tools.

This is the **source repository**: SDK implementation, sample apps, tests, and release tooling live here.

| Repository                                                 | Purpose                                                                            |
|------------------------------------------------------------|------------------------------------------------------------------------------------|
| [kourier-kmp](https://github.com/dev-shushant/kourier-kmp) | Develop, test, and build the SDK                                                   |
| [kourier](https://github.com/dev-shushant/kourier)         | Consumer documentation, public Maven artifacts, Swift package, and binary releases |

**Adding Kourier to an app?** Start with the [Android and iOS integration guide](https://github.com/dev-shushant/kourier#readme). Choose a version from the [published releases](https://github.com/dev-shushant/kourier/releases). The source version in [version.properties](version.properties) describes this checkout and is not a guarantee that the same version has been published.

## Contents

- [Capabilities](#capabilities)
- [Source architecture](#source-architecture)
- [Build and run locally](#build-and-run-locally)
- [Integration entry points](#integration-entry-points)
- [Publishing](#publishing)
- [Contributing](#contributing)
- [License](#license)

## Capabilities

- Inspect request and response headers, bodies, status, timings, and captured call stacks.
- Open the Compose inspector through a floating bubble, notification, shake gesture, or API call.
- Observe request counts, active requests, and errors through live telemetry.
- Mask configured headers, query parameters, and JSON fields; bound captured payloads and retained transactions.
- Export diagnostic text, HAR, and cURL representations of captured traffic.
- Use `kourier-noop` for Android release builds where inspection should be disabled.

Capture applies to clients wired to a Kourier integration. See the consumer guide for setup, permissions, configuration, and platform behavior.

## Source architecture

| Module / directory             | Responsibility                                                                              |
|--------------------------------|---------------------------------------------------------------------------------------------|
| `kourier-core`                 | `KourierCore`, configuration, transaction models, masking, event flow, and export utilities |
| `kourier-storage`              | SQLDelight SQLite persistence and in-memory storage                                         |
| `kourier-interceptor-okhttp`   | Android OkHttp interceptor; also used with Retrofit                                         |
| `kourier-interceptor-ktor`     | Ktor client plugin with Android and iOS source sets                                         |
| `kourier-interceptor-darwin`   | iOS URLSession interception through `NSURLProtocol`                                         |
| `kourier-ui`                   | Shared Compose inspector and platform triggers                                              |
| `kourier-android`              | Android facade and public interceptor dependencies                                          |
| `kourier-ios`                  | Static `KourierIos` XCFramework facade, exporting the Darwin integration                    |
| `kourier-noop`                 | Release stubs for Android integration                                                       |
| `Sources/KourierSwift`         | Swift convenience APIs included in the distribution Swift package                           |
| `sample-android`               | Android app using the local SDK modules                                                     |
| `samples/sample-ios`           | SwiftUI sample and Xcode project                                                            |
| `scripts/publish_optimized.sh` | Maven and XCFramework build, staging, and distribution                                      |

The capture flow is **network integration → KourierCore → storage and event flow → inspector and telemetry**. Core initialization precedes request recording. Shared code lives in `commonMain`; platform implementations live in `androidMain` and `iosMain` (the Android facade uses `src/main`).

The Android facade exposes configuration and interceptors while keeping storage and UI implementation dependencies internal. iOS hosts import `KourierIos`; the `Kourier` Swift package product additionally includes `KourierSwift` helpers.

## Build and run locally

### Requirements

- JDK 21 and the checked-in Gradle wrapper.
- Android SDK platform 37. The current Android modules and sample require API 29 or later.
- macOS for iOS builds; full Xcode selected as the active developer directory for XCFramework linking and running the iOS sample.
- iOS targets are `iosArm64` and `iosSimulatorArm64`; Intel iOS simulators are not configured.

Dependency versions are defined in [gradle/libs.versions.toml](gradle/libs.versions.toml). The current checkout uses Kotlin 2.4.10, AGP 9.4.0, Compose Multiplatform 1.12.0, and Ktor 3.1.3. Keep Ktor at the repository's pinned version: the documented 3.2.0 DEX issue affects Android builds below API 34.

The generated Swift manifest declares iOS 15. Verify the built framework's actual deployment requirement against the selected Xcode SDK before release; the manifest alone does not change the binary's minimum OS.

### Get the source

```bash
git clone https://github.com/dev-shushant/kourier-kmp.git
cd kourier-kmp
```

Configure your Android SDK through `local.properties` (`sdk.dir=...`) or your existing Android development environment.

### Android sample

```bash
./gradlew :sample-android:assembleDebug
```

Open the project in Android Studio to run `sample-android` on an emulator or device. It uses local project dependencies, so SDK edits are included without publishing.

### Focused verification

Run the checks relevant to your change:

```bash
# Unit tests
./gradlew test
./gradlew :kourier-core:testDebugUnitTest
./gradlew :kourier-interceptor-okhttp:test

# Android compilation
./gradlew :kourier-interceptor-ktor:compileKotlinAndroid
./gradlew :kourier-ui:compileKotlinAndroid

# iOS Kotlin compilation (.klib, without linking an XCFramework)
./gradlew :kourier-ios:compileKotlinIosSimulatorArm64
./gradlew :kourier-interceptor-darwin:compileKotlinIosSimulatorArm64
```

### iOS framework and sample

```bash
# Release framework only (also used by the publisher)
./gradlew :kourier-ios:assembleKourierIosReleaseXCFramework

# All configured framework variants
./gradlew :kourier-ios:assembleKourierIosXCFramework

open samples/sample-ios/KourierSampleApp.xcodeproj
```

The release framework is generated under `kourier-ios/build/XCFrameworks/release/`. Check the sample's framework reference and deployment target when changing Xcode or SDK versions.

An iOS host embedding the inspector needs `CADisableMinimumFrameDurationOnPhone = true` in `Info.plist`. Manual framework integration also needs SQLite and libc++ linkage (`-lsqlite3 -lc++`); the `Kourier` Swift package product includes those linker settings through its Swift target.

## Integration entry points

Use these source files when changing the public API:

- [Android facade](kourier-android/src/main/kotlin/dev/shushant/kourier/android/Kourier.kt): initialization and inspector controls.
- [Android telemetry](kourier-android/src/main/kotlin/dev/shushant/kourier/android/KourierTelemetry.kt): public stats APIs.
- [Core configuration](kourier-core/src/commonMain/kotlin/dev/shushant/kourier/core/config): builder, trigger styles, and masking settings.
- [Swift helpers](Sources/KourierSwift/Kourier.swift): array redaction helpers, Combine telemetry, and SwiftUI presentation binding.
- [iOS sample](samples/sample-ios/KourierSampleApp/Sources/KourierSampleApp.swift): host integration and Kotlin-to-Swift bridging examples.

Keep Android release stubs aligned with public API changes. Full consumer examples and configuration documentation are maintained in the [publishing README](https://github.com/dev-shushant/kourier#readme).

## Publishing

[version.properties](version.properties) supplies `VERSION_NAME`, the Maven `GROUP`, and `GITHUB_REPO` (the distribution destination). [build.gradle.kts](build.gradle.kts) defines publishable Maven modules and repositories. [The GitHub Actions workflow](.github/workflows/publish.yml) runs the optimized script on `v*` tags or manual dispatch.

### Distribution layout

| Destination in `dev-shushant/kourier` | Contents                                                                                                 |
|---------------------------------------|----------------------------------------------------------------------------------------------------------|
| `main`                                | Generated `Package.swift`, `Sources/KourierSwift`, license, and independently maintained consumer README |
| `mvn-repo`                            | Public Maven repository, retaining older artifact versions                                               |
| Release `v<VERSION_NAME>`             | `KourierIos.xcframework.zip` referenced by the Swift manifest, with its SHA-256 checksum                 |
| GitHub Packages                       | Additional Maven publication; upload failure is non-fatal in this script                                 |

The public Maven URL is `https://raw.githubusercontent.com/dev-shushant/kourier/mvn-repo`. Swift Package Manager uses `https://github.com/dev-shushant/kourier.git`.

**README ownership:** Edit this README for source development instructions. Edit the publishing repository's README directly for consumer documentation. The release script does not generate, copy, delete, or version-rewrite either README. A new distribution branch needs its consumer README added separately.

### Local packaging

```bash
# Build, test, and stage artifacts without remote publication
./scripts/publish_optimized.sh --dry-run

# Choose a Gradle worker limit
./scripts/publish_optimized.sh --dry-run --workers 4
```

Local output includes `build/repo`, `build/dist-repo-staging`, and the zipped release XCFramework. The script also updates the source `Package.swift` with the generated URL and checksum, including during a dry run. `--version` updates `version.properties` even during a dry run; review these local changes before committing.

### Release

Set `DIST_REPO_TOKEN` (preferred) or `GH_PAT` with write access to the distribution repository and package publishing access if using GitHub Packages. The script also accepts `GITHUB_TOKEN` or a token from `gh auth token`; the source repository's default Actions token does not provide cross-repository write access. Install the `gh` CLI to create the GitHub release and upload its XCFramework asset.

```bash
# Publish the version already configured in version.properties
./scripts/publish_optimized.sh

# Set and publish a new version
./scripts/publish_optimized.sh --version X.Y.Z
```

Use a new release version: the script force-updates version tags and replaces an existing GitHub release for that version. A tag-triggered workflow reads `version.properties`; it does not derive the SDK version from the triggering tag, so keep them aligned.

| Option              | Behavior                                                                                |
|---------------------|-----------------------------------------------------------------------------------------|
| `--dry-run`         | Build and stage without remote publication                                              |
| `--no-push`         | Build and stage without GitHub Packages uploads, distribution pushes, tags, or releases |
| `--version X.Y.Z`   | Update the source version before building                                               |
| `--skip-tests`      | Omit the script's `test` task                                                           |
| `--include-samples` | Include the Android sample project in the Gradle invocation                             |
| `--skip-samples`    | Exclude the sample project (default)                                                    |
| `--workers N`       | Set Gradle parallelism; defaults to detected logical cores                              |

Sample inclusion does not explicitly run `assembleDebug`; use the sample build command above when validating its APK. Optional 7-Zip accelerates archive compression; otherwise the script uses `zip`.

## Contributing

1. Make SDK changes in this source repository and follow [AGENTS.md](AGENTS.md) for platform constraints.
2. Keep facade APIs, interceptors, and no-op stubs consistent where applicable.
3. Run focused compilation/tests and validate the affected sample for UI or integration changes.
4. Update source documentation here and consumer documentation in the publishing repository when their respective behavior changes.
5. Include the problem, resulting behavior, and verification performed in your pull request.

## License

Copyright 2026 Shushant Tiwari. Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
