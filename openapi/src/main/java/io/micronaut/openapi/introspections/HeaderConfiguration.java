package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.models.headers.Header;

@Introspected(classes = {
		Header.class,
})
public class HeaderConfiguration {
}
