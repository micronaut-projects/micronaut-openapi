plugins {
    id("io.micronaut.build.internal.openapi-base")
    id("io.micronaut.build.internal.base-module")
    `java-test-fixtures`
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

micronautBuild {
    descriptor {
        parentModuleId = "io.micronaut.openapi:micronaut-openapi-annotations"
    }
}
dependencies {
    implementation("io.micronaut:micronaut-module-info:5.0.0-M20")
}
