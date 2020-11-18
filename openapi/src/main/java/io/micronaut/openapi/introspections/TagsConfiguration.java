package io.micronaut.openapi.introspections;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.models.tags.Tag;

@Introspected(classes = {
		Tag.class,
})
public class TagsConfiguration {
}
