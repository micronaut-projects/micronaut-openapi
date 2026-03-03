plugins {
    id("io.micronaut.build.internal.bom")
}

micronautBom {
    suppressions {
        acceptedVersionRegressions.add("freemarker")
        acceptedLibraryRegressions.add("freemarker")
        acceptedVersionRegressions.add("swagger-compat")
        acceptedLibraryRegressions.add("swagger")
        acceptedVersionRegressions.add("slf4j")
        acceptedLibraryRegressions.add("slf4j-nop")

        acceptedVersionRegressions.add("jakarta-validation-api")
        acceptedLibraryRegressions.add("jakarta-validation-api")
        dependencies.add("io.swagger.core.v3:swagger-core:2.2.43")
        dependencies.add("io.swagger.core.v3:swagger-models:2.2.43")
        dependencies.add("org.jsoup:jsoup:1.22.1")

    }
}
