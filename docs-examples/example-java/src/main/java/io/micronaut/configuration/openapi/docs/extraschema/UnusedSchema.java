package io.micronaut.configuration.openapi.docs.extraschema;

// tag::imports[]
import io.micronaut.openapi.annotation.OpenAPIExtraSchema;
// end::imports[]

// tag::clazz[]
@OpenAPIExtraSchema
class UnusedSchema {

    public String field1;
}
// end::clazz[]
