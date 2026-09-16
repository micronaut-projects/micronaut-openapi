package io.micronaut.configuration.openapi.docs.generics

import io.micronaut.configuration.openapi.docs.schemas.Pet
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.swagger.v3.oas.annotations.media.Schema

@Controller("/named")
class NamedResponseController {

    // tag::method[]
    @Put("/")
    @Schema(name = "ResponseOfPet")
    fun updatePet(pet: Pet): Response<Pet> {
        return Response(pet)
    }
    // end::method[]
}
