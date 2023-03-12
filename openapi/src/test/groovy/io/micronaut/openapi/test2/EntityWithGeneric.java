package io.micronaut.openapi.test2;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class EntityWithGeneric<T, R> {

    private final T fieldA;

    public EntityWithGeneric(final T fieldA) {
        this.fieldA = fieldA;
    }

    public T getFieldA() {
        return fieldA;
    }
}
