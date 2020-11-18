package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;


import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Introspected(classes = {
		OAuthFlow.class,
		OAuthFlows.class,
		Scopes.class,
		SecurityRequirement.class,
		SecurityScheme.class,
})
public class SecurityConfiguration {
}
