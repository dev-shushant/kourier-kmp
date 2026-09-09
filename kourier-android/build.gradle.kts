plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

/**
 * kourier-android — the ONLY dependency a host Android app ever adds.
 *
 * Public surface (visible to the host app):
 *   • dev.shushant.kourier.android.Kourier                — init / showUI / hideUI
 *   • dev.shushant.kourier.android.KourierTelemetry       — live stats StateFlow
 *   • dev.shushant.kourier.android.KourierStats           — public stats data class
 *   • dev.shushant.kourier.android.ui.KourierFloatingBubble — draggable Compose bubble
 *   • KourierOkHttpInterceptor                  — host wires into OkHttpClient
 *   • KourierKtorPlugin                         — host installs into Ktor HttpClient
 *
 * Everything else (kourier-ui, kourier-core, kourier-storage) is internal
 * and NOT reachable from the host app's compile classpath.
 */
android {
    namespace = "dev.shushant.kourier.android"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
            }
        }
    }
}

dependencies {
    // ── Internal — completely hidden from the host app ────────────────────
    // kourier-core is api() because KourierConfig.Builder is part of Kourier.init()'s
    // public lambda signature — the host app writes the builder DSL directly.
    api(project(":kourier-core"))
    implementation(project(":kourier-storage"))
    implementation(project(":kourier-ui"))
    implementation(libs.kotlinx.coroutines.android)

    // Compose for KourierFloatingBubble (implementation — Compose runtime
    // is already on the host's classpath via its own compose dependency)
    implementation("androidx.compose.runtime:runtime:1.6.8")
    implementation("androidx.compose.ui:ui:1.6.8")

    // ── Public — host app instantiates these directly ─────────────────────
    api(project(":kourier-interceptor-okhttp"))
    api(project(":kourier-interceptor-ktor"))

    // OkHttp and Ktor provided by the host app at runtime
    compileOnly(libs.okhttp)
    compileOnly(libs.ktor.client.core)
}
