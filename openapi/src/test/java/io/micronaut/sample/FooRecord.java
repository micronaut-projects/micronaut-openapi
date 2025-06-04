package io.micronaut.sample;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record FooRecord(
    String foo,
    String bar
) {
}
