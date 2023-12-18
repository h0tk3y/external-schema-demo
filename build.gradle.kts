plugins {
    alias(libs.plugins.kotlin.jvm)
    id("application")
}

dependencies {
    implementation(libs.kson)
    implementation(libs.ast)
    implementation(libs.clikt)
}

application {
    mainClass.set("MainKt")
    this.applicationName = "external-schema-demo"
}