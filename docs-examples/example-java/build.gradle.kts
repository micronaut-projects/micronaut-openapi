import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.micronautOpenapi)

    compileOnly(mn.micronaut.inject.java)
    compileOnly(projects.micronautOpenapiAnnotations)

    implementation(mn.micronaut.http)
    implementation(mn.reactor)
    implementation(mnValidation.validation)

    runtimeOnly(mnLogging.logback.classic)
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
