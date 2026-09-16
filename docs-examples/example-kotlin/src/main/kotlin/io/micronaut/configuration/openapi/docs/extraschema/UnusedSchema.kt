package io.micronaut.configuration.openapi.docs.extraschema

// tag::imports[]
import io.micronaut.openapi.annotation.OpenAPIExtraSchema
// end::imports[]

// tag::clazz[]
@OpenAPIExtraSchema
class UnusedSchema {

    var field1: String? = null
}
// end::clazz[]
