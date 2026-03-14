plugins {
    id("io.micronaut.build.internal.openapi-base")
    id("io.micronaut.build.internal.module")
}

repositories {
    mavenCentral()
    google()
}

val libs = versionCatalogs.named("libs")

dependencies {
    implementation("io.micronaut:micronaut-module-info:${libs.findVersion("micronaut").get()}")
}

micronautBuild {
    descriptor {
        parentModuleId = "io.micronaut.openapi:micronaut-openapi-annotations"
    }
}
