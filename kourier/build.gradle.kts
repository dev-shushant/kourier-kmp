plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

/**
 * kourier — umbrella module for Kotlin Multiplatform consumers.
 *
 * A KMP app adds ONE dependency (`dev.shushant.kourier:kourier`) and gets capture
 * (Ktor plugin, Darwin NSURLProtocol), storage and the inspector UI. Everything is
 * resolved as klibs and compiled into the host's own framework, so there is exactly one
 * Compose runtime and one KourierCore singleton. Do NOT also link KourierIos.xcframework.
 */
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "dev.shushant.kourier.umbrella"
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
            baseName = "Kourier"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":kourier-core"))
            api(project(":kourier-interceptor-ktor"))
            api(project(":kourier-ui"))
            implementation(project(":kourier-storage"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            api(project(":kourier-android"))
            api(project(":kourier-interceptor-okhttp"))
            implementation(libs.kotlinx.coroutines.android)
        }
        iosMain.dependencies {
            api(project(":kourier-interceptor-darwin"))
        }
    }
}

// The KMP "android" publication defaults to artifactId `kourier-android`, which collides with the
// legacy :kourier-android module (same Maven coordinates -> one overwrites the other, and GitHub
// Packages answers 409). Give the umbrella's Android variant a distinct artifactId.
afterEvaluate {
    publishing {
        publications.withType<MavenPublication>().matching { it.name == "android" }.configureEach {
            artifactId = "kourier-umbrella-android"
        }
    }
}
