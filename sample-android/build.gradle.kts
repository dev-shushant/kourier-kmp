plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

layout.buildDirectory.set(layout.projectDirectory.dir("build"))

android {
    namespace = "dev.shushant.kourier.sample.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.shushant.kourier.sample.android"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    attributesSchema {
        attribute(Attribute.of("artifactType", String::class.java)) {
            disambiguationRules.add(ArtifactTypeDisambiguationRule::class.java)
        }
    }

    // Local Kourier dependency — ONLY kourier-android is needed
    implementation(project(":kourier-android"))

    // App's own OkHttp / Ktor runtime (Kourier interceptors adapt to whatever version is here)
    implementation(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material:material:1.6.8")
    implementation("androidx.compose.foundation:foundation:1.6.8")
}

class ArtifactTypeDisambiguationRule : AttributeDisambiguationRule<String> {
    override fun execute(details: MultipleCandidatesDetails<String>) {
        if (details.candidateValues.contains("jar")) {
            details.closestMatch("jar")
        } else if (details.candidateValues.contains("android-classes-directory")) {
            details.closestMatch("android-classes-directory")
        }
    }
}
