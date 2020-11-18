package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

@Introspected(classes = {
		Components.class,
		ExternalDocumentation.class,
		OpenAPI.class,
		Operation.class,
		PathItem.class,
		Paths.class,
})
public class ModelConfiguration {
}
