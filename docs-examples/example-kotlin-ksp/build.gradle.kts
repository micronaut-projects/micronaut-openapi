import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    id("io.micronaut.build.internal.kotlin-ksp")
}

repositories {
    mavenCentral()
}

dependencies {

    ksp(mn.micronaut.inject.kotlin)
    ksp(mnValidation.micronaut.validation)
    ksp(projects.micronautOpenapi)

    compileOnly(projects.micronautOpenapiAnnotations)
    compileOnly(mn.micronaut.inject.kotlin)

    implementation(mn.micronaut.http)
    implementation(mn.reactor)
    implementation(mnValidation.validation)
    implementation(mn.kotlin.stdlib.asProvider())
    implementation(mn.kotlin.reflect)
    implementation(mnTest.junit.platform.suite)

    runtimeOnly(mnLogging.logback.classic)
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

ext["skipDocumentation"] = true
