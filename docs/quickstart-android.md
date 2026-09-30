# AppArmorX Resilience — Android quickstart

AppArmorX Resilience, formerly Kourier, provides on-device network inspection. Existing coordinates and APIs remain compatible. Choose a published version from the [distribution releases](https://github.com/dev-shushant/kourier/releases).

Add the public repository to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://raw.githubusercontent.com/dev-shushant/kourier/mvn-repo")
    }
}
```

Use the inspector for debug and the existing no-op artifact for release:

```kotlin
dependencies {
    debugImplementation("dev.shushant.kourier:kourier-android:<version>")
    releaseImplementation("dev.shushant.kourier:kourier-noop:<version>")
}
```

Initialize once in `Application.onCreate()`:

```kotlin
import dev.shushant.kourier.android.Kourier

Kourier.init(this) {
    redactHeaders("Authorization", "Cookie", "Set-Cookie", "X-Api-Key")
    redactPayloadKeys("password", "token", "secret")
    redactQueryParams("token", "apiKey")
}
```

Attach the interceptor to the client used by the app:

```kotlin
import dev.shushant.kourier.interceptor.okhttp.KourierOkHttpInterceptor
import okhttp3.OkHttpClient

val client = OkHttpClient.Builder()
    .addInterceptor(KourierOkHttpInterceptor())
    .build()
```

Open with `Kourier.showUI()` or a configured trigger. Request Android 13+ notification permission if using the notification trigger. Only clients wired to an integration are captured. Fault injection and portable scenarios are planned, not included in this quickstart.

See the [consumer guide](https://github.com/dev-shushant/kourier#readme), [local sample](../sample-android/README.md), and [brand transition](rebranding.md).
