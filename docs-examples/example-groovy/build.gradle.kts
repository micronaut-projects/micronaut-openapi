plugins {
    id("io.micronaut.build.internal.openapi-example-groovy")
}

dependencies {
    // used by the @OpenAPIInclude, @Secured and JAXBElement examples
    compileOnly(mn.micronaut.http.server)
    compileOnly(mn.micronaut.management)
    compileOnly(mnSecurity.micronaut.security)
    compileOnly(libs.jakarta.xml.bind.api)
}
