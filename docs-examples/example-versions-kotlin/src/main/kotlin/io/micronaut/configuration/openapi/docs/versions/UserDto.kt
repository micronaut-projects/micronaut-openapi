package io.micronaut.configuration.openapi.docs.versions

// tag::imports[]
import jakarta.validation.constraints.NotNull
// end::imports[]

// tag::clazz[]
class UserDto {

    var name: String? = null
    var age: Int = 0
    var secondName: String? = null
    @field:NotNull
    var address: String? = null
}
// end::clazz[]
