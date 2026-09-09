import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

/**
 * kourier-ios — the ONLY framework an iOS host app ever imports.
 *
 * Public surface (visible to the host app via KourierIos.xcframework):
 *   • dev.shushant.kourier.ios.Kourier                    — init / showUI / hideUI
 *   • KourierURLSessionConfiguration            — host wires into URLSession
 *
 * Everything else (KourierUI, KourierCore, KourierStorage) is hidden behind
 * implementation() and is NOT reachable as a separate framework by the host.
 */
kotlin {
    val xcf = XCFramework("KourierIos")

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KourierIos"
            isStatic = true
            // Flatten all implementation() deps into a single framework so the
            // host only needs to link KourierIos.xcframework.
            export(project(":kourier-interceptor-darwin"))
            xcf.add(this)
        }
    }

    sourceSets {
        iosMain.dependencies {
            // ── Public — re-exported in the KourierIos framework ───────────────
            api(project(":kourier-interceptor-darwin"))

            // ── Internal — merged but NOT separately visible to the host ───────
            implementation(project(":kourier-core"))
            implementation(project(":kourier-storage"))
            implementation(project(":kourier-ui"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
