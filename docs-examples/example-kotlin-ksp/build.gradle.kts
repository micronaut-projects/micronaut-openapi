import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    id("io.micronaut.build.internal.kotlin-ksp")
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
    testImplementation(mnTest.junit.platform.suite)


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

ext["skipDocumentation"] = true

tasks.withType<Test> {
    failOnNoDiscoveredTests = false
}
