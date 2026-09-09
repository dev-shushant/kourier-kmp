plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "dev.shushant.kourier.noop"
        compileSdk = 37
        minSdk = 29

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KourierNoop"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            compileOnly(libs.ktor.client.core)
        }
        androidMain.dependencies {
            compileOnly(libs.okhttp)
        }
    }
}
