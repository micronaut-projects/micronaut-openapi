package io.micronaut.configuration.openapi.docs.generics

import io.micronaut.configuration.openapi.docs.schemas.Pet
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put

// tag::clazz[]
@Controller
class MyController {

    @Put("/")
    Response<Pet> updatePet(Pet pet) {
        Response<Pet> response = new Response<>()
        response.result = pet
        return response
    }
}
// end::clazz[]
