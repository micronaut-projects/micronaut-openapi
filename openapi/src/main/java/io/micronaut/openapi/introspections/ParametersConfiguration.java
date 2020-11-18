package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;

import io.swagger.v3.oas.models.parameters.CookieParameter;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

@Introspected(classes = {
		CookieParameter.class,
		HeaderParameter.class,
		Parameter.class,
		PathParameter.class,
		QueryParameter.class,
		RequestBody.class,
})
public class ParametersConfiguration {
}
