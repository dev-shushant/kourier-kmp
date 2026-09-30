plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KourierInterceptorDarwin"
            isStatic = true
        }
    }

    sourceSets {
        iosTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":kourier-storage"))
        }
        commonMain.dependencies {
            implementation(project(":kourier-core"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
