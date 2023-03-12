package io.micronaut.openapi.test3;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class EntityWithGeneric<T, R> {

    private final T fieldC;

    public EntityWithGeneric(final T fieldC) {
        this.fieldC = fieldC;
    }

    public T getFieldC() {
        return fieldC;
    }
}
