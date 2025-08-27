import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    groovy
}

repositories {
    mavenCentral()
}

dependencies {

    testCompileOnly(mn.micronaut.inject.groovy)
    testCompileOnly(projects.micronautOpenapi)

    testImplementation(mn.micronaut.http)
    testImplementation(mnGroovy.micronaut.runtime.groovy)
    testImplementation(mnValidation.validation)
    testImplementation(mn.reactor)

    testRuntimeOnly(mnLogging.logback.classic)
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
