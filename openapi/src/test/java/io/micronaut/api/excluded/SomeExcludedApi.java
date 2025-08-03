package io.micronaut.api.excluded;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
public class SomeExcludedApi {

    @Get("/user")
    public void getUser() {

    }
}
