package io.micronaut.configuration.openapi.docs.naming;

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema;
// end::imports[]

// tag::clazz[]
@Schema(description = "A pet") // <1>
class Pet {
}
// end::clazz[]
