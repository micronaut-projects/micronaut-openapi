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

// The Python compiler does not expose the location of the generated files to the OpenAPI visitor,
// so the project directory (openapi.properties, src/main/resources/application.yml) is passed explicitly.
tasks.withType<PythonCompile>().configureEach {
    systemProperties.put("micronaut.openapi.project.dir", projectDir.toString())
}

// TODO(python): the Python compiler writes one GraalPy virtual file system per compilation and the generated
// Python shims of the main and test compilations shadow each other at runtime, so the main Python sources (the
// documented types) are compiled together with the test sources into the test output.
tasks.named("compilePython") {
    enabled = false
}
tasks.named<PythonCompile>("compileTestPython") {
    source.from((sourceSets.main.get().extensions.getByName("python") as SourceDirectorySet).sourceDirectories)
}
