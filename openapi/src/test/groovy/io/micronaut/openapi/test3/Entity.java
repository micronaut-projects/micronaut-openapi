package io.micronaut.openapi.test3;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class Entity {

    private final String fieldC;

    public Entity(final String fieldC) {
        this.fieldC = fieldC;
    }

    public String getFieldC() {
        return fieldC;
    }
}
