package io.micronaut.openapi.test.api

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Controller
class TestApi {

    @Get("/test")
    @Produces(MediaType.TEXT_PLAIN)
    fun index(@QueryValue types: List<LivestreamLivecenterFilterType>?) = ""

    @Serdeable
    @Schema(name = "TypeDto")
    enum class LivestreamLivecenterFilterType {
        EPISODE,
        SCHEDULED_LIVESTREAM,
    }
}
