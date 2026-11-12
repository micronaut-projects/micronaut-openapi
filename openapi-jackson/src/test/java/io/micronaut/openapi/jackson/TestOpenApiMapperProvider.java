package io.micronaut.openapi.jackson;

import io.micronaut.json.JsonMapper;
import io.micronaut.openapi.mapper.OpenApiMapperProvider;
import io.micronaut.openapi.mapper.OpenApiMapperType;
import io.micronaut.openapi.mapper.OpenApiVersion;

/**
 * A second provider, registered for the tests only, that relies on the interface's default methods.
 */
public class TestOpenApiMapperProvider implements OpenApiMapperProvider {

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public JsonMapper getMapper(OpenApiMapperType type, OpenApiVersion version) {
        return new JacksonOpenApiMapperProvider().getMapper(type, version);
    }
}
