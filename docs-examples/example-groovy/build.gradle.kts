import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    groovy
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(mn.micronaut.inject.groovy)
    compileOnly(projects.micronautOpenapi)

    implementation(mn.micronaut.http)
    implementation(mnGroovy.micronaut.runtime.groovy)
    implementation(mnValidation.validation)
    implementation(mn.reactor)
    implementation(mnTest.junit.platform.suite)

    runtimeOnly(mnLogging.logback.classic)
}

tasks.test {
    jvmArgs("-Duser.country=US", "-Duser.language=en")
    testLogging {
        exceptionFormat = FULL
    }
    failFast = true
}

tasks.withType(GroovyCompile::class) {
    groovyOptions.forkOptions.jvmArgs = listOf("-Dgroovy.parameters=true")
}

ext["skipDocumentation"] = true
