package io.micronaut.configuration.openapi.docs.controllers;

// tag::imports[]
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.validation.Validated;
import io.reactivex.Single;

// end::imports[]
// tag::clazz[]
@Validated
@Controller("/")
public class HelloController {

    /**
     * @param name The person's name
     * @return The greeting
     */
    @Get(uri = "/hello/{name}", produces = MediaType.TEXT_PLAIN)
    public Single<String> index(String name) {
        return Single.just("Hello " + name + "!");
    }
}
// end::clazz[]