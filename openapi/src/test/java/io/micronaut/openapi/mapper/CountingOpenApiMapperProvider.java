package io.micronaut.openapi.mapper;

import io.micronaut.json.JsonMapper;
import io.micronaut.openapi.jackson.JacksonOpenApiMapperProvider;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A provider for tests that delegates to Jackson and counts how often the visitor asks it for a mapper.
 */
public class CountingOpenApiMapperProvider implements OpenApiMapperProvider {

    public static final String NAME = "counting";
    public static final AtomicInteger MAPPER_REQUESTS = new AtomicInteger();

    private final JacksonOpenApiMapperProvider delegate = new JacksonOpenApiMapperProvider();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public JsonMapper getMapper(OpenApiMapperType type, OpenApiVersion version) {
        MAPPER_REQUESTS.incrementAndGet();
        return delegate.getMapper(type, version);
    }
}
