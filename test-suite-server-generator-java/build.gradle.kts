import io.micronaut.build.internal.openapi.OpenApiGeneratorTask

plugins {
    id("io.micronaut.build.internal.openapi-java-generator-test-suite")
    groovy
}

description = """
This project tests that the generated server sources can be compiled and
that tests can be ran with Micronaut 4
"""

dependencies {

    annotationProcessor(mnValidation.micronaut.validation.processor)
    annotationProcessor(mnSerde.micronaut.serde.processor)
    annotationProcessor(projects.micronautOpenapi)

    compileOnly(mn.jackson.annotations)

    implementation(projects.micronautOpenapi)
    implementation(mn.micronaut.http)
    implementation(mnSerde.micronaut.serde.api)
    implementation(mn.jakarta.annotation.api)
    implementation(mnValidation.micronaut.validation)
    implementation(mnData.micronaut.data.runtime)
    implementation(mnReactor.micronaut.reactor)

    runtimeOnly(mnLogging.logback.classic)
    runtimeOnly(mn.snakeyaml)

    testAnnotationProcessor(projects.micronautOpenapi)

    testCompileOnly(mn.micronaut.inject.groovy.test)
    testCompileOnly(mn.micronaut.inject.java.test)

    testImplementation(mnTest.micronaut.test.spock)
    testImplementation(mn.micronaut.http.client)
    testImplementation(projects.micronautOpenapiCommon)

    testRuntimeOnly(mn.micronaut.json.core)
    testRuntimeOnly(mnSerde.micronaut.serde.jackson)
    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mn.snakeyaml)
    testRuntimeOnly(mnTest.junit.platform.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
}

sourceSets {
    test {
        java.srcDir("src/test/groovy")
    }
}

tasks.named("generateOpenApi", OpenApiGeneratorTask::class) {
    generatorKind = "server"
    openApiDefinition = layout.projectDirectory.file("spec.yaml")
    outputKinds = listOf("models", "apis", "modelDocs", "supportingFiles", "modelTests", "apiTests")
    parameterMappings = listOf(
        // Pageable parameter
        mapOf("name" to "page", "location" to "QUERY", "mappedType" to "io.micronaut.data.model.Pageable"),
        mapOf("name" to "size", "location" to "QUERY", "mappedType" to "io.micronaut.data.model.Pageable"),
        mapOf("name" to "sortOrder", "location" to "QUERY", "mappedType" to "io.micronaut.data.model.Pageable"),
        // Ignored header
        mapOf("name" to "ignored-header", "location" to "HEADER"),
        // Custom filtering header
        mapOf("name" to "Filter", "location" to "HEADER", "mappedType" to "io.micronaut.openapi.test.filter.MyFilter"),
    )
    responseBodyMappings = listOf(
        // Response with Last-Modified header mapping
        mapOf("headerName" to "Last-Modified", "mappedBodyType" to "io.micronaut.openapi.test.dated.DatedResponse"),
        // Response with Page body
        mapOf("headerName" to "X-Page-Number", "mappedBodyType" to "io.micronaut.data.model.Page", "isListWrapper" to "true"),
        mapOf("headerName" to "X-Page-Count", "mappedBodyType" to "io.micronaut.data.model.Page", "isListWrapper" to "true"),
        mapOf("headerName" to "X-Total-Count", "mappedBodyType" to "io.micronaut.data.model.Page", "isListWrapper" to "true"),
        mapOf("headerName" to "X-Page-Size", "mappedBodyType" to "io.micronaut.data.model.Page", "isListWrapper" to "true"),
        // Ignored header - Does not wrap the response in HttpResponse
        mapOf("headerName" to "ignored-header"),
    )
    nameMapping = mapOf("test" to "changedTest")
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
    options.isIncremental = true
    options.isFork = true
    options.compilerArgs = mutableListOf(
        "-parameters",
        "-Xlint:unchecked",
        "-Xlint:deprecation",
    )
    options.forkOptions.jvmArgs = mutableListOf(
        "-Dmicronaut.openapi.views.spec=swagger-ui.enabled=true",
        "-Dmicronaut.openapi.environments=local",
        "-Dmicronaut.application.name=openapi-micronaut",
    )
}
