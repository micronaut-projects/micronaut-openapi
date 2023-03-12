package io.micronaut.openapi.test1;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class EntityWithGeneric<T> {

    private final T fieldB;

    public EntityWithGeneric(final T fieldB) {
        this.fieldB = fieldB;
    }

    public T getFieldB() {
        return fieldB;
    }
}
