package io.micronaut.configuration.openapi.docs.resolution

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema
// end::imports[]

// tag::clazz[]
@Schema(description="Pet") // <1>
class Pet {
}
// end::clazz[]
