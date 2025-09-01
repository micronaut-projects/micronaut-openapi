package io.micronaut.openapi.test.api;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Controller
public class TestApi {

    @Get("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public String index(@QueryValue @Nullable List<LivestreamLivecenterFilterType> types) {
        return "";
    }

    @Serdeable
    @Schema(name = "TypeDto")
    public enum LivestreamLivecenterFilterType {
        EPISODE,
        SCHEDULED_LIVESTREAM,
    }
}
