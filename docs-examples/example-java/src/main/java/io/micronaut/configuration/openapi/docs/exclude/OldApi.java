package io.micronaut.configuration.openapi.docs.exclude;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/old")
public class OldApi {

    @Get
    public String index() {
        return "old";
    }
}
