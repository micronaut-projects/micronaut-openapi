package io.micronaut.configuration.openapi.docs.schemas

// tag::imports[]
import io.swagger.v3.oas.annotations.media.Schema
// end::imports[]

// tag::clazz[]
@Schema(name = "MyPet", description = "Pet description") // <1>
class Pet {

    var type: PetType? = null

    /**
     * The age
     */
    @get:Schema(description = "Pet age", maximum = "20") // <2>
    var age: Int = 0

    @get:Schema(description = "Pet name", maxLength = 20)
    var name: String? = null
}
// end::clazz[]
