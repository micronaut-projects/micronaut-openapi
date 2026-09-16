plugins {
    id("io.micronaut.build.internal.openapi-example-base")
    groovy
}

val mn = versionCatalogs.named("mn")
val mnGroovy = versionCatalogs.named("mnGroovy")
val mnValidation = versionCatalogs.named("mnValidation")
val mnLogging = versionCatalogs.named("mnLogging")
val mnTest = versionCatalogs.named("mnTest")

dependencies {
    compileOnly(mn.findLibrary("micronaut-inject-groovy").get())
    compileOnly(project(":micronaut-openapi"))

    implementation(mn.findLibrary("micronaut-http").get())
    implementation(mnGroovy.findLibrary("micronaut-runtime-groovy").get())
    implementation(mnValidation.findLibrary("validation").get())
    implementation(mn.findLibrary("reactor").get())

    runtimeOnly(mnLogging.findLibrary("logback-classic").get())

    testCompileOnly(mn.findLibrary("micronaut-inject-groovy").get())
    testImplementation(mnTest.findLibrary("micronaut-test-spock").get())
}

tasks.withType<GroovyCompile>().configureEach {
    groovyOptions.forkOptions.jvmArgs = listOf("-Dgroovy.parameters=true")
}
