@file:Suppress("UnstableApiUsage")

pluginManagement {
    fun RepositoryHandler.setup() {
        mavenCentral()
        if (this == pluginManagement.repositories) {
            gradlePluginPortal()
        }
        if (this == dependencyResolutionManagement.repositories) {
            maven("https://jitpack.io")
            maven("/Users/sergey.igushkin/Projects/Gradle/gradle/kotlin-static-object-notation/build/publication")
            mavenLocal()
        }
    }
    repositories.setup()
    dependencyResolutionManagement {
        repositories.setup()
        versionCatalogs { 
            create("libs") {
                val kotlinVersion = version("kotlin", "1.9.20")
                plugin("kotlin.jvm", "org.jetbrains.kotlin.jvm").versionRef(kotlinVersion)
                
                library("kson", "com.h0tk3y", "kotlin-static-object-notation").withoutVersion()
                
                library("ast", "kotlinx.ast:grammar-kotlin-parser-antlr-kotlin:0.1.0")
                library("clikt", "com.github.ajalt.clikt:clikt:4.2.1")
            }
        }
    }
}

includeBuild("kotlin-static-object-notation")