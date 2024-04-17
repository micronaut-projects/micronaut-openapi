plugins {
    id("io.micronaut.build.internal.openapi-test-java")
}

dependencies {
    testAnnotationProcessor(mnJaxrs.micronaut.jaxrs.processor)
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testAnnotationProcessor(projects.micronautOpenapi)

    testCompileOnly(projects.micronautOpenapiAnnotations)

    testImplementation(mnJaxrs.micronaut.jaxrs.server)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.snakeyaml)
}
