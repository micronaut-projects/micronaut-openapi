plugins {
    id("io.micronaut.build.internal.binary-compatibility-check")
    id("io.micronaut.build.internal.openapi-simple-module")
}

dependencies {
    api(mn.micronaut.json.core)
    api(libs.managed.swagger.models)
    testImplementation(mnTest.junit.jupiter.api)
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
