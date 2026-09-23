package io.micronaut.configuration.openapi.docs.schemas

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

@Controller("/pets")
class PetController {

    @Get("/{name}")
    fun getPet(name: String): Pet {
        val pet = Pet()
        pet.name = name
        return pet
    }

    @Post
    fun savePet(@Body @MyAnn pet: Pet): Pet = pet
}
