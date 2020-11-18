package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@Introspected(classes = {
		ApiResponse.class,
		ApiResponses.class,
})
public class ResponsesConfiguration {
}
