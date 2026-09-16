package io.micronaut.configuration.openapi.docs.schemas

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

@Controller("/pets")
class PetController {

    @Get("/{name}")
    Pet getPet(String name) {
        Pet pet = new Pet()
        pet.name = name
        return pet
    }

    @Post
    Pet savePet(@Body @MyAnn Pet pet) {
        return pet
    }
}
