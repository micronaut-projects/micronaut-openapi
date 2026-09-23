package io.micronaut.configuration.openapi.docs.dynamicrefs;

import java.util.List;

// tag::clazz[]
class Page<T> {
    public List<T> items;
    public int total;
}
// end::clazz[]
