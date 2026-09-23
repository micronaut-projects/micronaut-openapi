plugins {
    id("io.micronaut.build.internal.openapi-example-java")
}

dependencies {
    // application.yml is read at compile time, which needs a YAML parser on the annotation processor path
    annotationProcessor(mn.snakeyaml)
}
