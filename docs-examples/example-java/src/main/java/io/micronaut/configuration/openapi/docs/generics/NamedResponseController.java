package io.micronaut.configuration.openapi.docs.generics;

import io.micronaut.configuration.openapi.docs.schemas.Pet;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller("/named")
class NamedResponseController {

    // tag::method[]
    @Put("/")
    @Schema(name = "ResponseOfPet")
    public Response<Pet> updatePet(Pet pet) {
        Response<Pet> response = new Response<>();
        response.setResult(pet);
        return response;
    }
    // end::method[]
}
