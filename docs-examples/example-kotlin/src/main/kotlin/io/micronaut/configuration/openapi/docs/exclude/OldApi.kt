package io.micronaut.configuration.openapi.docs.exclude

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/old")
class OldApi {

    @Get
    fun index(): String = "old"
}
