plugins {
    id("io.micronaut.build.internal.openapi-example-python")
}

dependencies {
    // used by the @OpenAPIInclude, @Secured and JAXBElement examples; the generated Python modules
    // look the Java classes up at runtime, so they are runtime (not compileOnly) dependencies
    implementation(mn.micronaut.http.server)
    implementation(mn.micronaut.management)
    implementation(mnSecurity.micronaut.security)
    implementation(libs.jakarta.xml.bind.api)
}
