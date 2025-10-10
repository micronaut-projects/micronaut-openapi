plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.micronaut.gradle.plugin)
    implementation(libs.micronaut.shared.settings)
}
