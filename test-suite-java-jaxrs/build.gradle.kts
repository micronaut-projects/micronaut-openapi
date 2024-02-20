plugins {
    id("io.micronaut.build.internal.openapi-test-java")
}

dependencies {
    testAnnotationProcessor(mnJaxrs.micronaut.jaxrs.processor)
    testImplementation(mnJaxrs.micronaut.jaxrs.server)
    testImplementation(mn.micronaut.http.server.netty)

    testImplementation(mn.snakeyaml)

    testImplementation(mn.micronaut.http.client)

    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)

    testAnnotationProcessor(projects.micronautOpenapi)
    testCompileOnly(projects.micronautOpenapiAnnotations)
}
