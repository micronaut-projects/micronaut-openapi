package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.links.LinkParameter;

@Introspected(classes = {
		Link.class,
		LinkParameter.class,
})
public class LinksConfiguration {
}
