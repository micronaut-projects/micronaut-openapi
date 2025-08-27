import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {

    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautOpenapi)

    testCompileOnly(mn.micronaut.inject.java)
    testCompileOnly(projects.micronautOpenapiAnnotations)

    testImplementation(mn.micronaut.http)
    testImplementation(mn.reactor)
    testImplementation(mnValidation.validation)

    testRuntimeOnly(mnLogging.logback.classic)
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.add("-parameters")
}

tasks.test {
    jvmArgs("-Duser.country=US", "-Duser.language=en")
    testLogging {
        exceptionFormat = FULL
    }
    failFast = true
}

ext["skipDocumentation"] = true
