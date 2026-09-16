package io.micronaut.configuration.openapi.docs.config

// tag::imports[]
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
// end::imports[]

// tag::clazz[]
/**
 * User main operations.
 */
@Controller
class UserOperationsController {

    @Get("/read/{id}")
    fun read(id: String): String = "OK!"

    @Post("/save/{id}")
    fun save2(id: String, body: Any): String = "OK!"

    @Post("/save")
    fun save(body: Any): String = "OK!"
}
// end::clazz[]
