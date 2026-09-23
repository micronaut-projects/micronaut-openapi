package io.micronaut.configuration.openapi.docs.decorator;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class MyRequest {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
