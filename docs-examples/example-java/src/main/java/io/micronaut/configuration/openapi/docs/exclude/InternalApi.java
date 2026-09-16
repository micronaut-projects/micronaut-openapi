package io.micronaut.configuration.openapi.docs.exclude;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/internal")
public class InternalApi {

    @Get
    public String index() {
        return "internal";
    }
}
