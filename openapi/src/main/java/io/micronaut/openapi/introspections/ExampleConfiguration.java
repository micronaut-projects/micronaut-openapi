package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.models.examples.Example;

@Introspected(classes = {
		Example.class,
})
public class ExampleConfiguration {
}
