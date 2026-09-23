plugins {
    id("io.micronaut.build.internal.openapi-example-kotlin")
}

dependencies {
    // application.yml is read at compile time, which needs a YAML parser on the symbol processor path
    ksp(mn.snakeyaml)
}

ksp {
    arg("micronaut.openapi.project.dir", projectDir.toString())
}
