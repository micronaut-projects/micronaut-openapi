plugins {
    id("io.micronaut.build.internal.openapi-example-python")
}

dependencies {
    // application.yml is read at compile time, which needs a YAML parser on the compiler path
    implementation(mn.snakeyaml)
}
