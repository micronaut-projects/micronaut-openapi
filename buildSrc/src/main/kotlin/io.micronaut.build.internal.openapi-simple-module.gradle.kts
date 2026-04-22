plugins {
    id("io.micronaut.build.internal.openapi-base")
    id("io.micronaut.build.internal.base-module")
    `java-test-fixtures`
}

val libs = versionCatalogs.named("libs")

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

dependencies {
    implementation("io.micronaut:micronaut-module-info:${libs.findVersion("micronaut").get()}")
}

micronautBuild {
    descriptor {
        parentModuleId = "io.micronaut.openapi:micronaut-openapi-annotations"
    }
}
