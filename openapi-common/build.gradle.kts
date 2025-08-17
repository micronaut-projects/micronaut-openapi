plugins {
    id("io.micronaut.build.internal.binary-compatibility-check")
    id("io.micronaut.build.internal.openapi-simple-module")
}

dependencies {
    api(mn.jackson.databind)
    api(mn.jackson.dataformat.yaml)
    api(mn.jackson.datatype.jsr310)
    api(libs.managed.swagger.models)
}

configurations.configureEach {

    exclude(group = "io.micronaut", module = "micronaut-inject-java")
    exclude(group = "io.micronaut", module = "micronaut-inject")
    exclude(group = "io.micronaut", module = "micronaut-core-bom")
}
