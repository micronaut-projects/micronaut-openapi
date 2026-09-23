import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

// Shared setup of the docs-examples/example-* projects backing the snippet:: macros of the user guide.
plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

val libs = versionCatalogs.named("libs")
val mn = versionCatalogs.named("mn")
val mnTest = versionCatalogs.named("mnTest")
val mnLogging = versionCatalogs.named("mnLogging")

dependencies {
    // the generated OpenAPI documents are asserted by the tests
    testImplementation(project(":micronaut-openapi-common"))
    testImplementation(mnTest.findLibrary("micronaut-test-junit5").get())
    testImplementation(mnTest.findLibrary("junit-jupiter-api").get())
    testRuntimeOnly(mnTest.findLibrary("junit-jupiter-engine").get())
    testRuntimeOnly(mnTest.findLibrary("junit-platform-launcher").get())
    testRuntimeOnly(mnLogging.findLibrary("logback-classic").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-Duser.country=US", "-Duser.language=en")
    testLogging {
        exceptionFormat = FULL
    }
    failFast = true
}

extra["skipDocumentation"] = true
