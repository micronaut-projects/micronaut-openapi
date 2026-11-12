plugins {
    id("io.micronaut.build.internal.openapi-simple-module")
}

dependencies {
    api(projects.micronautOpenapiCommon)
    api(mn.jackson.databind)
    api(mn.jackson.dataformat.yaml)
    api(libs.managed.swagger.models)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
}

configurations.configureEach {

    exclude(group = "io.micronaut", module = "micronaut-inject-java")
    exclude(group = "io.micronaut", module = "micronaut-core-bom")
}

micronautBuild {
    // TODO: re-enable once 8.0.0 is released. openapi-jackson is new and the Jackson classes moved out of
    // openapi-common into it in 8.0.0, so there is no compatible baseline to compare against.
    binaryCompatibility {
        enabled.set(false)
    }
}
