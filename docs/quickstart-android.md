# Quickstart — Android (network module integration)

This guide shows the minimal steps to add Kourier into an Android network module (e.g., an OkHttp-based networking module). Kourier is intended to be used inside your network layer — there's no need to change app launch logic.

1) Add dependencies (module build.gradle.kts)
implementation("dev.shushant:kourier-core:<version>")
implementation("dev.shushant:kourier-interceptor-okhttp:<version>")

2) Create / wire the OkHttp client
import dev.shushant.kourier.interceptor.okhttp.KourierOkHttpInterceptor

val client = OkHttpClient.Builder()
  .addInterceptor(KourierOkHttpInterceptor.create(/* optional config */))
  .build()

3) Initialize core if needed (optional for most interceptors)
import dev.shushant.kourier.core.KourierCore

// Call once in your network module initialization
KourierCore.initialize(
  config = KourierCore.Config(
    // platform-specific configuration if required
  )
)

4) Verify in tests
- Add a unit test in your network module that uses MockWebServer to assert interceptor behavior (retries, headers, caching).

Notes
- Replace <version> with the library version (match Maven Central coordinates).
- Kourier is Kotlin Multiplatform — this interceptor is designed for JVM/Android use.

