pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    includeBuild("build-logic")
}

plugins {
    id("io.micronaut.build.shared.settings") version "7.5.0"
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "openapi-parent"

include(
        "openapi",
        "openapi-annotations",
        "openapi-bom",
        "openapi-common",
        "openapi-adoc",
        "openapi-generator",
        "docs-examples:example-groovy",
        "docs-examples:example-java",
        "docs-examples:example-kotlin-kapt",
        "docs-examples:example-kotlin-ksp",
        "test-suite-java-client-generator",
        "test-suite-java-jaxrs",
        "test-suite-java-spring",
        "test-suite-java-server-generator",
        "test-suite-kotlin-kapt-client-generator",
        "test-suite-kotlin-kapt-server-generator",
        "test-suite-kotlin-kapt-spring",
        "test-suite-kotlin-ksp-client-generator",
        "test-suite-kotlin-ksp-server-generator",
        "test-suite-kotlin-ksp-spring",
        "test-suite-generator-util",
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

micronautBuild {
    useStandardizedProjectNames = true
    importMicronautCatalog()
    importMicronautCatalog("micronaut-security")
    importMicronautCatalog("micronaut-serde")
    importMicronautCatalog("micronaut-rxjava2")
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
