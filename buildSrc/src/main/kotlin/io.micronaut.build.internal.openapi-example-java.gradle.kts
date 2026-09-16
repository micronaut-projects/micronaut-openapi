plugins {
    id("io.micronaut.build.internal.openapi-example-base")
}

val mn = versionCatalogs.named("mn")
val mnValidation = versionCatalogs.named("mnValidation")
val mnLogging = versionCatalogs.named("mnLogging")

dependencies {
    annotationProcessor(mn.findLibrary("micronaut-inject-java").get())
    annotationProcessor(mnValidation.findLibrary("micronaut-validation").get())
    annotationProcessor(project(":micronaut-openapi"))

    compileOnly(mn.findLibrary("micronaut-inject-java").get())
    compileOnly(project(":micronaut-openapi-annotations"))

    implementation(mn.findLibrary("micronaut-http").get())
    implementation(mn.findLibrary("reactor").get())
    implementation(mnValidation.findLibrary("validation").get())

    runtimeOnly(mnLogging.findLibrary("logback-classic").get())

    testAnnotationProcessor(mn.findLibrary("micronaut-inject-java").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}
