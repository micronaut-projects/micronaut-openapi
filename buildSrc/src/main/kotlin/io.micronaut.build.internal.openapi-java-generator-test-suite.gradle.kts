import io.micronaut.build.internal.openapi.OpenApiGeneratorTask
import io.micronaut.gradle.MicronautRuntime
import io.micronaut.gradle.MicronautTestRuntime
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    id("io.micronaut.build.internal.openapi-base")
    id("io.micronaut.minimal.application")
}

repositories {
    mavenCentral()
}

val libs = versionCatalogs.named("libs")

val openapiGenerator = configurations.create("openapiGenerator") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    openapiGenerator(project(":test-suite-generator-util"))
    constraints {
        implementation("io.micronaut:micronaut-http-client:${libs.findVersion("micronaut").get()}")
    }
}

val openapiGenerate = tasks.register("generateOpenApi", OpenApiGeneratorTask::class) {
    lang = "java"
    generatedAnnotation = true
    clientId = "myClient"
    clientPath = true
    ksp = false
    useOneOfInterfaces = false
    classpath.from(openapiGenerator)
    openApiDefinition.convention(layout.projectDirectory.file("petstore.json"))
    outputDirectory.convention(layout.buildDirectory.dir("generated/openapi"))
    generatorKind.convention("client")
    outputKinds.convention(listOf("models", "apis", "apiDocs", "modelDocs", "supportingFiles", "modelTests", "apiTests"))
    parameterMappings.convention(emptyList())
    responseBodyMappings.convention(emptyList())
}

sourceSets {
    main {
        java.srcDir(openapiGenerate.map { it.generatedSourcesDirectory })
    }
    test {
        java.srcDir(openapiGenerate.map { it.generatedTestSourcesDirectory })
    }
}

micronaut {
    version = libs.findVersion("micronaut-platform").get().toString()
    runtime = MicronautRuntime.NETTY
    testRuntime = MicronautTestRuntime.JUNIT_5
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = FULL
    }
}
