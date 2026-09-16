package io.micronaut.configuration.openapi.docs.config;

// tag::imports[]
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
// end::imports[]

// tag::clazz[]
/**
 * User main operations.
 */
@Controller
public class UserOperationsController {

    @Get("/read/{id}")
    public String read(String id) {
        return "OK!";
    }

    @Post("/save/{id}")
    public String save2(String id, Object body) {
        return "OK!";
    }

    @Post("/save")
    public String save(Object body) {
        return "OK!";
    }
}
// end::clazz[]
