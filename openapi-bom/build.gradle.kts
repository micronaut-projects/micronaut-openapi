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
    }
}
