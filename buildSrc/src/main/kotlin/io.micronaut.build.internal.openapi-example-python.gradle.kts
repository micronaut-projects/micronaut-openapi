import io.micronaut.build.python.PythonCompile

plugins {
    id("io.micronaut.build.internal.openapi-example-base")
    id("io.micronaut.build.internal.python")
}

val mn = versionCatalogs.named("mn")
val mnValidation = versionCatalogs.named("mnValidation")

dependencies {
    // The Python compiler takes the (jar-resolved) compile classpath as its annotation processor path,
    // so the OpenAPI visitor is a regular (not annotationProcessor) dependency.
    implementation(project(":micronaut-openapi"))
    implementation(project(":micronaut-openapi-annotations"))
    implementation(mn.findLibrary("micronaut-inject-python").get())
    implementation(mn.findLibrary("micronaut-context-python").get())
    implementation(mn.findLibrary("micronaut-runtime").get())

    implementation(mn.findLibrary("micronaut-http").get())
    implementation(mn.findLibrary("reactor").get())
    implementation(mnValidation.findLibrary("validation").get())

    testImplementation(mn.findLibrary("micronaut-inject-python-test").get())
}

tasks.withType<Test>().configureEach {
    systemProperty("micronaut.python.pool.enabled", "false")
}

// The OpenAPI visitor cannot derive the project directory (openapi.properties, src/main/resources/application.yml)
// from the generated files of a Python compilation, so it is passed explicitly like in the other languages (see the
// Java convention plugin).
tasks.withType<PythonCompile>().configureEach {
    compilerArgs.add("-Amicronaut.openapi.project.dir=$projectDir")
}
