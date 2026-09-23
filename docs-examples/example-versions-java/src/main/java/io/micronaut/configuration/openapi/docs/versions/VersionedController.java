package io.micronaut.configuration.openapi.docs.versions;

// tag::imports[]
import io.micronaut.core.version.annotation.Version;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
// end::imports[]

// tag::clazz[]
@Controller("/versioned")
public class VersionedController {

    @Version("1")
    @Get("/hello")
    public String helloV1() {
        return "helloV1";
    }

    @Version("2")
    @Post("/hello")
    public String helloV2(UserDto userDto) {
        return "helloV2";
    }

    @Post("/common")
    public String common() {
        return null;
    }
}
// end::clazz[]
