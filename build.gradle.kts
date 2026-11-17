import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
}

val versionProperties = Properties().apply {
    val versionFile = rootProject.file("version.properties")
    if (versionFile.exists()) {
        versionFile.inputStream().use { load(it) }
    }
}

val sdkVersionName: String = versionProperties.getProperty("VERSION_NAME", "0.0.1")
val sdkGroup: String = versionProperties.getProperty("GROUP", "dev.shushant.kourier")
val githubRepo: String = (project.findProperty("GITHUB_REPOSITORY") as String?
    ?: versionProperties.getProperty("GITHUB_REPO", "dev-shushant/kourier"))

allprojects {
    group = sdkGroup
    version = sdkVersionName
}

val publishableModules = setOf(
    "kourier",
    "kourier-android",
    "kourier-noop",
    "kourier-core",
    "kourier-storage",
    "kourier-ui",
    "kourier-interceptor-okhttp",
    "kourier-interceptor-ktor",
    "kourier-interceptor-darwin"
)

// Modules that must stay Compose-free so capture/storage can be reused without a UI runtime.
val composeFreeModules = setOf(
    "kourier-core",
    "kourier-storage",
    "kourier-interceptor-ktor",
    "kourier-interceptor-darwin",
    "kourier-interceptor-okhttp"
)

subprojects {
    if (name in composeFreeModules) {
        afterEvaluate {
            if (plugins.hasPlugin("org.jetbrains.compose") || plugins.hasPlugin("org.jetbrains.kotlin.plugin.compose")) {
                throw GradleException("$name must stay Compose-free (module boundary rule). Move UI code to :kourier-ui.")
            }
        }
    }
    if (name in publishableModules) {
        apply(plugin = "maven-publish")

        configure<PublishingExtension> {
            repositories {
                // 1. Direct local repository for zero-credential Maven branch (mvn-repo)
                maven {
                    name = "DistributionRepo"
                    url = uri("${rootProject.layout.buildDirectory.get().asFile}/repo")
                }
                // 2. GitHub Packages (for Android Maven artifacts)
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/$githubRepo")
                    credentials {
                        username = project.findProperty("GITHUB_ACTOR") as String?
                            ?: project.findProperty("gpr.user") as String?
                            ?: System.getenv("GITHUB_ACTOR")
                            ?: ""
                        password = project.findProperty("GITHUB_TOKEN") as String?
                            ?: project.findProperty("gpr.key") as String?
                            ?: System.getenv("GITHUB_TOKEN")
                            ?: ""
                    }
                }
            }

            publications.withType<MavenPublication> {
                pom {
                    name.set(project.name)
                    description.set("Kourier - Enterprise Network Inspection SDK for Android & iOS")
                    url.set("https://github.com/$githubRepo")
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("shushant")
                            name.set("Shushant Tiwari")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/$githubRepo.git")
                        developerConnection.set("scm:git:ssh://github.com:$githubRepo.git")
                        url.set("https://github.com/$githubRepo")
                    }
                }
            }
        }

        tasks.withType<PublishToMavenRepository>().configureEach {
            // Exclude iOS-specific klib publications from GitHub Packages only. They MUST still be
            // published to the DistributionRepo (mvn-repo branch), otherwise KMP consumers cannot
            // resolve `<module>-iosarm64` / `<module>-iossimulatorarm64` artifacts.
            if (name.endsWith("ToGitHubPackagesRepository") &&
                (name.contains("Ios", ignoreCase = true) || name.contains("Darwin", ignoreCase = true))
            ) {
                enabled = false
            }
        }
    }
}


