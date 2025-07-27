package io.micronaut.api.internal;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
public class InternalApi {

    @Get("/internal/get-key")
    public void getKey() {

    }
}
