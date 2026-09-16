import java
from java.lang import ClassLoader, String
from micronaut.openapi import OpenApiUtils

# TODO(python): java.type needed because `io.swagger.*` (any `io.` package other than `io.micronaut`) cannot be imported at runtime
OpenAPI = java.type("io.swagger.v3.oas.models.OpenAPI")


def load(file_name: str):
    """Loads the OpenAPI document generated at compile time into the class output."""
    stream = ClassLoader.getSystemResourceAsStream("META-INF/swagger/" + file_name)
    assert stream is not None, "Generated OpenAPI document not found: " + file_name
    try:
        text = String(stream.readAllBytes(), "UTF-8")
    finally:
        stream.close()
    return OpenApiUtils.getYamlMapper().readValue(text, OpenAPI)
