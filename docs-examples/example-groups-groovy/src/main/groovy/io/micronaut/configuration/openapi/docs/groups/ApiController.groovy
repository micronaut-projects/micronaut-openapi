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

    @OpenAPIGroup(exclude = "v2")
    @Get("/read/{id}")
    String read(String id) {
        return "OK!"
    }

    @OpenAPIGroup("v2")
    @Post("/save/{id}")
    String save2(String id, Object body) {
        return "OK!"
    }

    @OpenAPIGroup(["v1", "v2"])
    @Post("/save")
    String save(Object body) {
        return "OK!"
    }
}
// end::clazz[]
