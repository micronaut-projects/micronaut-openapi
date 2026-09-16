package io.micronaut.configuration.openapi.docs.extraschema

// tag::imports[]
import io.micronaut.openapi.annotation.OpenAPIExtraSchema
// end::imports[]

// tag::clazz[]
@OpenAPIExtraSchema(
    // classes to add
    classes = UnusedModel1,
    // excluded classes, which marked with `@OpenAPIExtraSchema` annotation
    excludeClasses = ExcludedModel,
    // exclude classes by packages
    excludePackages = "io.micronaut.configuration.openapi.docs.extraschema.exclude",
    // include classes by packages
    packages = "io.micronaut.configuration.openapi.docs.extraschema.extra"
)
class Application {
}
// end::clazz[]
