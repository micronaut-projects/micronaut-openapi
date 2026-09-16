package io.micronaut.configuration.openapi.docs.naming

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema
// end::imports[]

// tag::clazz[]
class Owner {

    @get:Schema(description = "Pet that is a bird") // <2>
    var bird: Pet? = null

    @get:Schema(description = "Pet that is a cat") // <3>
    var cat: Pet? = null

    @get:Schema(name = "Dog", description = "Pet that is a dog") // <4>
    var dog: Pet? = null
}
// end::clazz[]
