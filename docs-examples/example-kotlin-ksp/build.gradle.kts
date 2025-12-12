import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    alias(mn.plugins.kotlin.jvm)
    alias(mn.plugins.kotlin.allopen)
    alias(mn.plugins.ksp)
}

repositories {
    mavenCentral()
}

dependencies {

    kspTest(mn.micronaut.inject.kotlin)
    kspTest(mnValidation.micronaut.validation)
    kspTest(projects.micronautOpenapi)

    testCompileOnly(projects.micronautOpenapiAnnotations)
    testCompileOnly(mn.micronaut.inject.kotlin)

    testImplementation(mn.micronaut.http)
    testImplementation(mn.reactor)
    testImplementation(mnValidation.validation)
    testImplementation(mn.kotlin.stdlib.asProvider())
    testImplementation(mn.kotlin.reflect)

    testRuntimeOnly(mnLogging.logback.classic)
}

ksp {
    arg("micronaut.openapi.project.dir", projectDir.toString())
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
