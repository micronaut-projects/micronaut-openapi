plugins {
    id("io.micronaut.build.internal.binary-compatibility-check")
    id("io.micronaut.build.internal.openapi-simple-module")
}

dependencies {
    api(mn.jackson.databind)
    api(mn.jackson.dataformat.yaml)
    api(libs.managed.swagger.models)
    implementation("io.micronaut:micronaut-module-info:5.0.0-M16")
    testImplementation(mnTest.junit.platform.launcher)
}

configurations.configureEach {

    exclude(group = "io.micronaut", module = "micronaut-inject-java")
    exclude(group = "io.micronaut", module = "micronaut-inject")
    exclude(group = "io.micronaut", module = "micronaut-core-bom")
}
