package io.micronaut.configuration.openapi.docs.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertiesConfigurationTest {

    @Test
    void placeholdersAreExpanded() {
        var info = OpenApiSpec.load("hello-world-v1.1.yml").getInfo();

        assertEquals("Hello World", info.getTitle());
        assertEquals("A nice API", info.getDescription());
        assertEquals("v1.1", info.getVersion());
        assertEquals("Fred", info.getContact().getName());
        assertEquals("Apache 2.0", info.getLicense().getName());
    }
}
