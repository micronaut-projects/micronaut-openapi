import io.micronaut.gradle.MicronautRuntime
import io.micronaut.gradle.MicronautTestRuntime
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    id("io.micronaut.minimal.application") version libs.versions.micronaut.gradle.plugin.get()
    java
}

repositories {
    mavenCentral()
}

micronaut {
    version = libs.versions.micronaut.platform.get()
    runtime = MicronautRuntime.NETTY
    testRuntime = MicronautTestRuntime.JUNIT_5
}

dependencies {
    annotationProcessor(mnJaxrs.micronaut.jaxrs.processor)
    annotationProcessor(mnSerde.micronaut.serde.processor)
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.micronautOpenapi)

    compileOnly(projects.micronautOpenapiAnnotations)

    implementation(mnJaxrs.micronaut.jaxrs.server)
    implementation(mn.micronaut.http.server.netty)
    implementation(mnSerde.micronaut.serde.jackson)
    implementation(mn.snakeyaml)

    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mn.micronaut.http.client)

    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mnTest.junit.platform.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = FULL
    }
}
