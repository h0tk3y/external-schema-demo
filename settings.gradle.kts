@file:Suppress("UnstableApiUsage")

pluginManagement {
    fun RepositoryHandler.setup() {
        mavenCentral()
        if (this == pluginManagement.repositories) {
            gradlePluginPortal()
        }
        if (this == dependencyResolutionManagement.repositories) {
            maven {
                name = "Gradle public repository"
                url = uri("https://repo.gradle.org/artifactory/libs-snapshots/")
                content {
                    includeGroup("org.gradle")
                }
            }
        }
    }
    repositories.setup()
    dependencyResolutionManagement {
        repositories.setup()
        versionCatalogs { 
            create("libs") {
                val kotlinVersion = version("kotlin", "1.9.20")
                plugin("kotlin.jvm", "org.jetbrains.kotlin.jvm").versionRef(kotlinVersion)

                val declarativeDslCoreVersion = "8.8-20240222001206+0000"
                library("kson", "org.gradle", "gradle-declarative-dsl-core").version(declarativeDslCoreVersion)

                library("clikt", "com.github.ajalt.clikt:clikt:4.2.1")
            }
        }
    }
}