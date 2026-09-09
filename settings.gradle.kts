pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://raw.githubusercontent.com/dev-shushant/kourier/mvn-repo") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/dev-shushant/kourier/mvn-repo") }
    }
}

rootProject.name = "kourier-kmp"

include(":kourier-core")
include(":kourier-storage")
include(":kourier-interceptor-okhttp")
include(":kourier-interceptor-ktor")
include(":kourier-interceptor-darwin")
include(":kourier-ui")
include(":kourier-android")
include(":kourier-noop")
include(":kourier-ios")
val skipSamples = providers.gradleProperty("skipSamples").isPresent ||
    extra.properties.containsKey("skipSamples")
if (!skipSamples) {
    include(":sample-android")
}
