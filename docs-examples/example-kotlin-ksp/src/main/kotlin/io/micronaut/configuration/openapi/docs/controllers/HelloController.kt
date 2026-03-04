package io.micronaut.configuration.openapi.docs.controllers

// tag::imports[]
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import reactor.core.publisher.Mono

// end::imports[]
// tag::clazz[]
@Controller("/")
open class HelloController {

    /**
     * @param name The person's name
     * @return The greeting
     */
    @Get(uri = "/hello/{name}", produces = [MediaType.TEXT_PLAIN])
    open fun index(name: String): Mono<String> {
        return Mono.just("Hello $name!")
    }
}
// end::clazz[]