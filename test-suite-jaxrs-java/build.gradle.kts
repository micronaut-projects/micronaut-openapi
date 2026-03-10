plugins {
    id("io.micronaut.build.internal.openapi-test-java")
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

    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mn.micronaut.http.client)
    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.add("-parameters")
}

