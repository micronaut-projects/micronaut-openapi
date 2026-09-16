package io.micronaut.configuration.openapi.docs.exclude

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/internal")
class InternalApi {

    @Get
    fun index(): String = "internal"
}
