package io.micronaut.api;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
public class IncludedApi {

    @Get("/public/get-info")
    public void getInfo() {

    }
}
