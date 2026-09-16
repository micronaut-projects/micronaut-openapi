package io.micronaut.configuration.openapi.docs.generics

import io.micronaut.configuration.openapi.docs.schemas.Pet
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put

// tag::clazz[]
@Controller
class MyController {

    @Put("/")
    fun updatePet(pet: Pet): Response<Pet> {
        return Response(pet)
    }
}
// end::clazz[]
