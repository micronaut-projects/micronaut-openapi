package io.micronaut.configuration.openapi.docs.accessors;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/persons")
class PersonController {

    @Get("/{name}")
    public Person getPerson(String name) {
        return new Person(name, 1, 2);
    }
}
