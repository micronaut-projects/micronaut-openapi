plugins {
    id("io.micronaut.build.internal.openapi-example-kotlin")
}

ksp {
    arg("micronaut.openapi.project.dir", projectDir.toString())
}
