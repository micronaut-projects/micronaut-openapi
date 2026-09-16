package io.micronaut.configuration.openapi.docs.accessors

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/persons")
class PersonController {

    @Get("/{name}")
    fun getPerson(name: String): Person = Person(name, 1, 2)
}
