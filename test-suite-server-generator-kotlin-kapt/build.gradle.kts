import io.micronaut.build.internal.openapi.OpenApiGeneratorTask

plugins {
    id("io.micronaut.build.internal.openapi-kotlin-kapt-generator-test-suite")
    id("io.micronaut.build.internal.kotlin-kapt")
}

description = """
This project tests that the generated server sources can be compiled and
that tests can be ran with Micronaut 4
"""

dependencies {

    kapt(mnValidation.micronaut.validation.processor)
    kapt(mnSerde.micronaut.serde.processor)
    kapt(mn.micronaut.inject.kotlin)
    kapt(projects.micronautOpenapi)

    compileOnly(projects.micronautOpenapiAnnotations)
    compileOnly(mn.jackson.annotations)

    implementation(mn.micronaut.http)
    implementation(mnSerde.micronaut.serde.api)
    implementation(mn.micronaut.inject.kotlin)
    implementation(mn.jakarta.annotation.api)
    implementation(mnValidation.micronaut.validation)
    implementation(mnReactor.micronaut.reactor)
    implementation(mnData.micronaut.data.runtime)
    implementation(mn.kotlin.stdlib.asProvider())
    implementation(mn.kotlin.reflect)
    // Required when using useAuth=true
    implementation(mnSecurity.micronaut.security.oauth2)

    runtimeOnly(mnLogging.logback.classic)
    runtimeOnly(mnSerde.micronaut.serde.jackson)
    runtimeOnly(mn.snakeyaml)

    kaptTest(mnValidation.micronaut.validation.processor)
    kaptTest(mnSerde.micronaut.serde.processor)
    kaptTest(mn.micronaut.inject.kotlin)

    testCompileOnly(mn.micronaut.inject.kotlin.test)

    testImplementation(mn.micronaut.http.client)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mnTest.junit.jupiter.params)
    testImplementation(projects.micronautOpenapiCommon)

    testRuntimeOnly(mn.micronaut.json.core)
    testRuntimeOnly(mnSerde.micronaut.serde.jackson)
    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mn.snakeyaml)
    testRuntimeOnly(mnTest.junit.platform.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
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

kapt {
    arguments {
        arg("micronaut.openapi.project.dir", "$projectDir")
        arg("micronaut.openapi.environments", "local")
        arg("micronaut.openapi.views.spec", "swagger-ui.enabled=true")
    }
    useBuildCache = false
}

