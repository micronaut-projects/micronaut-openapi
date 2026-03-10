pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("io.micronaut.build.shared.settings") version "8.0.0-M17"
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "openapi-parent"

include(
    "docs-examples:example-groovy",
    "docs-examples:example-java",
    "docs-examples:example-kotlin-kapt",
    "docs-examples:example-kotlin-ksp",
    "openapi",
    "openapi-adoc",
    "openapi-annotations",
    "openapi-bom",
    "openapi-common",
//    "openapi-generator",
//    "test-suite-client-generator-java",
//    "test-suite-client-generator-kotlin-kapt",
//    "test-suite-client-generator-kotlin-ksp",
//    "test-suite-generator-util",
//    "test-suite-jaxrs-java",
//    "test-suite-server-generator-java",
//    "test-suite-server-generator-kotlin-kapt",
//    "test-suite-server-generator-kotlin-ksp",
//    "test-suite-spring-java",
//    "test-suite-spring-kotlin-kapt",
//    "test-suite-spring-kotlin-ksp",
)

micronautBuild {
    useStandardizedProjectNames = true
    importMicronautCatalog()
    importMicronautCatalog("micronaut-security")
    importMicronautCatalog("micronaut-views")
    importMicronautCatalog("micronaut-serde")
    importMicronautCatalog("micronaut-rxjava3")
    importMicronautCatalog("micronaut-reactor")
    importMicronautCatalog("micronaut-groovy")
    importMicronautCatalog("micronaut-validation")
    importMicronautCatalog("micronaut-data")
    importMicronautCatalog("micronaut-kotlin")
    importMicronautCatalog("micronaut-session")
    importMicronautCatalog("micronaut-jaxrs")
    importMicronautCatalog("micronaut-grpc")
    importMicronautCatalog("micronaut-spring")
}
