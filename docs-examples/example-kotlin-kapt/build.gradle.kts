import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    id("io.micronaut.build.internal.kotlin-kapt")
}

repositories {
    mavenCentral()
}

dependencies {

    kaptTest(mn.micronaut.inject.kotlin)
    kaptTest(mnValidation.micronaut.validation)
    kaptTest(projects.micronautOpenapi)

    testCompileOnly(projects.micronautOpenapiAnnotations)
    testCompileOnly(mn.micronaut.inject.kotlin)

    testImplementation(mn.micronaut.http)
    testImplementation(mn.reactor)
    testImplementation(mnValidation.validation)
    testImplementation(mn.kotlin.stdlib.asProvider())
    testImplementation(mn.kotlin.reflect)

    testRuntimeOnly(mnLogging.logback.classic)
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

ext["skipDocumentation"] = true
