plugins {
    id("io.micronaut.build.internal.openapi-example-base")
    id("io.micronaut.build.internal.kotlin-ksp")
}

val mn = versionCatalogs.named("mn")
val mnValidation = versionCatalogs.named("mnValidation")
val mnLogging = versionCatalogs.named("mnLogging")

dependencies {
    ksp(mn.findLibrary("micronaut-inject-kotlin").get())
    ksp(mnValidation.findLibrary("micronaut-validation").get())
    ksp(project(":micronaut-openapi"))

    compileOnly(project(":micronaut-openapi-annotations"))
    compileOnly(mn.findLibrary("micronaut-inject-kotlin").get())

    implementation(mn.findLibrary("micronaut-http").get())
    implementation(mn.findLibrary("reactor").get())
    implementation(mnValidation.findLibrary("validation").get())
    implementation(mn.findLibrary("kotlin-stdlib").get())
    implementation(mn.findLibrary("kotlin-reflect").get())

    runtimeOnly(mnLogging.findLibrary("logback-classic").get())

    kspTest(mn.findLibrary("micronaut-inject-kotlin").get())
}
