import io.micronaut.build.internal.openapi.RemoteDownloadTask

plugins {
    id("io.micronaut.build.internal.openapi-module")
}

dependencies {

    compileOnly(mn.micronaut.core.processor)
    compileOnly(projects.micronautOpenapiAdoc)

    implementation(mn.micronaut.core)
    implementation(mn.micronaut.inject)
    implementation(mn.micronaut.http)
    runtimeOnly(mn.snakeyaml)

    api(projects.micronautOpenapiAnnotations)
    api(projects.micronautOpenapiCommon)
    api(mn.jackson.databind)
    api(mn.jackson.dataformat.yaml)
    api(libs.managed.swagger.models)
    api(libs.managed.javadoc.parser)
    api(libs.managed.flexmark.html2md.converter) {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    api(libs.managed.flexmark.profile.pegdown) {
        exclude(group = "org.jetbrains", module = "annotations")
    }

    // this dependency needs to be updated manually. It's used by html2md
    api(libs.managed.jsoup)

    testImplementation(projects.micronautOpenapiAdoc)
    testImplementation(mnSession.micronaut.session)
    testImplementation(mn.micronaut.management)
    testImplementation(mn.micronaut.inject.kotlin)
    testImplementation(mn.micronaut.inject.kotlin.test)
    testImplementation(mn.micronaut.inject.groovy.test)
    testImplementation(mn.micronaut.inject.java.test)
    testImplementation(mn.micronaut.http.server)
    testImplementation(mn.snakeyaml)
    testImplementation(mnSecurity.micronaut.security)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnRxjava3.micronaut.rxjava3)
    testImplementation(mnData.micronaut.data.model)
    testImplementation(mnValidation.validation)
    testImplementation(mnGrpc.protobuf.java)
    testImplementation(libs.jspecify)
    testImplementation(libs.jdt.annotation)
    testImplementation(libs.android.annotation)
    testImplementation(libs.spotbugs.annotations)
    testImplementation(libs.guava)
    testImplementation(mn.kotlinx.coroutines.reactor)
    testRuntimeOnly(mnTest.junit.platform.launcher)

// uncomment it, if you want to test micronaut-openapi with spring-boot actuator locally
//    testAnnotationProcessor(mnSpring.micronaut.spring.annotation)
//    testAnnotationProcessor(mnSpring.micronaut.spring.web.annotation)
//    testAnnotationProcessor(mnSpring.micronaut.spring.boot.annotation)
//    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:${mnSpring.versions.spring.boot.get()}"))
//    testImplementation(libs.spring.boot.starter.web)
//    testImplementation(libs.spring.boot.starter.actuator)
//    testImplementation(libs.micrometer.registry.prometheus)
}

configurations {

    compileClasspath {
        exclude(group = "io.micronaut", module = "micronaut-core-bom")
    }

    runtimeClasspath {
        exclude(group = "io.micronaut", module = "micronaut-core-bom")
    }

    configureEach {
        exclude(group = "ch.qos.logback")
    }
}

tasks.test {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    maxHeapSize = "2048m"
}

val downloadResources = tasks.register("downloadResources") {
    description = "Downloads all external resources"
}

mapOf(
    "npm/swagger-ui@${libs.versions.js.swagger.ui.get()}/dist/swagger-ui-bundle.js" to listOf("SwaggerUIBundle", "swagger-ui/res/swagger-ui-bundle.js"),
    "npm/swagger-ui@${libs.versions.js.swagger.ui.get()}/dist/swagger-ui-standalone-preset.js" to listOf("SwaggerUIPreset", "swagger-ui/res/swagger-ui-standalone-preset.js"),
    "npm/swagger-ui@${libs.versions.js.swagger.ui.get()}/dist/swagger-ui.css" to listOf("SwaggerUICss", "swagger-ui/res/swagger-ui.css"),
    "npm/openapi-explorer@${libs.versions.js.openapi.explorer.get()}/dist/browser/openapi-explorer.min.js" to listOf("OpenApiExplorer", "openapi-explorer/res/openapi-explorer.min.js"),
    "npm/redoc@${libs.versions.js.redoc.get()}/bundles/redoc.standalone.js" to listOf("Redoc", "redoc/res/redoc.standalone.js"),
    "npm/rapidoc@${libs.versions.js.rapidoc.get()}/dist/rapidoc-min.js" to listOf("Rapidoc", "rapidoc/res/rapidoc-min.js"),
    "npm/rapipdf@${libs.versions.js.rapipdf.get()}/dist/rapipdf-min.js" to listOf("Rapipdf", "rapipdf/res/rapipdf-min.js"),
    "npm/@scalar/api-reference@${libs.versions.js.scalar.get()}/dist/browser/standalone.js" to listOf("Scalar", "scalar/res/standalone.js"),
).forEach {
    val taskName = "download${it.value[0]}"
    val download = tasks.register(taskName, RemoteDownloadTask::class) {
        baseUrl = "https://cdn.jsdelivr.net"
        contentPath = it.key
        outputPath = "templates/${it.value[1]}"
        outputDirectory = layout.buildDirectory.dir("downloads/$taskName")
    }
    downloadResources {
        dependsOn(download)
    }
    sourceSets {
        main {
            resources.srcDir(download)
        }
    }
}
