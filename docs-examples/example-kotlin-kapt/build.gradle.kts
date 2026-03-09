import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    alias(mn.plugins.kotlin.jvm)
    alias(mn.plugins.kotlin.allopen)
    alias(mn.plugins.kotlin.kapt)
}

repositories {
    mavenCentral()
}

dependencies {

    kapt(mn.micronaut.inject.kotlin)
    kapt(mnValidation.micronaut.validation)
    kapt(projects.micronautOpenapi)

    compileOnly(projects.micronautOpenapiAnnotations)
    compileOnly(mn.micronaut.inject.kotlin)

    implementation(mn.micronaut.http)
    implementation(mn.reactor)
    implementation(mnValidation.validation)
    implementation(mn.kotlin.stdlib.asProvider())
    implementation(mn.kotlin.reflect)

    runtimeOnly(mnLogging.logback.classic)
}

kapt {
    arguments {
        arg("micronaut.openapi.project.dir", projectDir.toString())
    }
}

tasks.test {
    jvmArgs("-Duser.country=US", "-Duser.language=en")
    testLogging {
        exceptionFormat = FULL
    }
    failFast = true
}

kotlin {
    jvmToolchain(21)
}
ext["skipDocumentation"] = true
