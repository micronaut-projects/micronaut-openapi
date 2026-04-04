plugins {
    id("io.micronaut.build.internal.openapi-base")
    id("io.micronaut.build.internal.module")
}

repositories {
    mavenCentral()
    google()
}
micronautBuild {
    descriptor {
        parentModuleId = "io.micronaut.openapi:micronaut-openapi-annotations"
    }
}
dependencies {
    implementation("io.micronaut:micronaut-module-info:5.0.0-M20")
}
