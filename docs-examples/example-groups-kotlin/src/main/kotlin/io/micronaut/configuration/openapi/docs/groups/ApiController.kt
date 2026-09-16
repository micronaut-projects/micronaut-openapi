package io.micronaut.configuration.openapi.docs.groups

// tag::imports[]
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.openapi.annotation.OpenAPIGroup
// end::imports[]

// tag::clazz[]
@Controller
class ApiController {

    @OpenAPIGroup(exclude = ["v2"])
    @Get("/read/{id}")
    fun read(id: String): String = "OK!"

    @OpenAPIGroup("v2")
    @Post("/save/{id}")
    fun save2(id: String, body: Any): String = "OK!"

    @OpenAPIGroup("v1", "v2")
    @Post("/save")
    fun save(body: Any): String = "OK!"
}
// end::clazz[]
