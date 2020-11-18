package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.models.callbacks.Callback;

@Introspected(classes = {
		Callback.class,
})
public class CallbackConfiguration {
}
