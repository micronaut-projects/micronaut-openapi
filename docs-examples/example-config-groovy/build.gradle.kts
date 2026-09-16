plugins {
    id("io.micronaut.build.internal.openapi-example-groovy")
}

dependencies {
    // application.yml is read at compile time, which needs a YAML parser on the compiler path
    compileOnly(mn.snakeyaml)
}
