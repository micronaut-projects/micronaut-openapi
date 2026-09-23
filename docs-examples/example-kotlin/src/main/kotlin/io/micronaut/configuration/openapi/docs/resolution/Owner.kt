package io.micronaut.configuration.openapi.docs.resolution

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema
// end::imports[]

// tag::clazz[]
class Owner {

    var cat: Pet? = null // <2>

    @get:Schema(name = "MyPet", description = "This is my pet") // <3>
    var dog: Pet? = null
}
// end::clazz[]
