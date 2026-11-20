package io.micronaut.openapi.jackson;

import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.swagger.core.util.PrimitiveType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.EncodingProperty;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips OpenAPI documents through the swagger-core Jackson serializers and deserializers copied into this module.
 */
class SwaggerCoreJacksonTest {

    private static final String DOC = """
        openapi: 3.0.1
        info:
          title: t
          version: "1"
        paths:
          /a:
            post:
              operationId: a
              callbacks:
                withExtension:
                  "{$request.body#/url}":
                    post:
                      operationId: cb
                  x-callback: 1
                refWithExtension:
                  $ref: "#/components/callbacks/C"
                  x-callback: 2
                ref:
                  $ref: "#/components/callbacks/C"
                plain:
                  "{$request.body#/other}":
                    get:
                      operationId: cb2
              requestBody:
                content:
                  multipart/form-data:
                    schema:
                      type: object
                    encoding:
                      file:
                        style: form
                        explode: true
                    examples:
                      one:
                        value: v
                      two:
                        $ref: "#/components/examples/E"
              responses:
                "200":
                  description: ok
                  headers:
                    X-Rate:
                      style: simple
                      schema:
                        type: integer
          x-paths: true
        components:
          examples:
            E:
              summary: s
              value:
                k: v
          callbacks:
            C:
              "{$url}":
                get:
                  operationId: c
        """;

    @Test
    void roundTripsOpenApi30Document() {
        for (ObjectMapper mapper : List.of(OpenApiUtils.YAML_MAPPER, OpenApiUtils.YAML_MAPPER_31)) {
            OpenAPI api = mapper.readValue(DOC, OpenAPI.class);

            Paths paths = api.getPaths();
            assertEquals(Map.of("x-paths", true), paths.getExtensions());
            Operation post = paths.get("/a").getPost();
            Map<String, Callback> callbacks = post.getCallbacks();
            assertEquals(Map.of("x-callback", 1), callbacks.get("withExtension").getExtensions());
            assertEquals("#/components/callbacks/C", callbacks.get("ref").get$ref());
            MediaType mediaType = post.getRequestBody().getContent().get("multipart/form-data");
            assertEquals(Encoding.StyleEnum.FORM, mediaType.getEncoding().get("file").getStyle());
            assertEquals("#/components/examples/E", mediaType.getExamples().get("two").get$ref());
            Header header = post.getResponses().get("200").getHeaders().get("X-Rate");
            assertEquals(Header.StyleEnum.SIMPLE, header.getStyle());

            String written = mapper.writeValueAsString(api);
            assertTrue(written.contains("x-paths: true"), written);
            assertTrue(written.contains("x-callback: 1"), written);
            assertTrue(written.contains("x-callback: 2"), written);
            assertTrue(written.contains("style: form"), written);
            assertTrue(written.contains("style: simple"), written);
            assertTrue(written.contains("$ref: \"#/components/examples/E\""), written);

            OpenAPI reread = mapper.readValue(written, OpenAPI.class);
            assertEquals(mapper.writeValueAsString(reread), written);
        }
    }

    @Test
    void writesExampleAndPathsVariants() {
        ObjectMapper mapper = OpenApiUtils.JSON_MAPPER;
        assertEquals("{\"$ref\":\"#/components/examples/E\"}", mapper.writeValueAsString(new Example().$ref("#/components/examples/E")));
        assertEquals("{\"summary\":\"s\",\"value\":null}", mapper.writeValueAsString(new Example().summary("s").value(null)));
        assertEquals("{\"/a\":{}}", mapper.writeValueAsString(new Paths().addPathItem("/a", new PathItem())));
        var mediaType = new MediaType().example(null);
        assertEquals("{\"example\":null}", mapper.writeValueAsString(mediaType));
    }

    @Test
    void readsAndRejectsStyleEnums() {
        ObjectMapper mapper = OpenApiUtils.JSON_MAPPER;
        assertEquals(EncodingProperty.StyleEnum.DEEPOBJECT, mapper.readValue("\"deepObject\"", EncodingProperty.StyleEnum.class));
        assertEquals(Encoding.StyleEnum.SPACE_DELIMITED, mapper.readValue("\"spaceDelimited\"", Encoding.StyleEnum.class));
        assertEquals(Header.StyleEnum.SIMPLE, mapper.readValue("\"simple\"", Header.StyleEnum.class));
        assertNull(mapper.readValue("null", Header.StyleEnum.class));
        // the deserializers copied from swagger-core reject unknown values with a plain RuntimeException
        assertThrows(RuntimeException.class, () -> mapper.readValue("\"nope\"", EncodingProperty.StyleEnum.class));
        assertThrows(RuntimeException.class, () -> mapper.readValue("\"nope\"", Encoding.StyleEnum.class));
        assertThrows(RuntimeException.class, () -> mapper.readValue("\"nope\"", Header.StyleEnum.class));
    }

    @Test
    void readsOpenApi31Schemas() {
        OpenAPI api = OpenApiUtils.JSON_MAPPER_31.readValue("""
            {"openapi":"3.1.0","info":{"title":"t","version":"1"},
             "components":{"schemas":{
               "S":{"type":["string","null"],"examples":["a"],"const":"a","$id":"s"},
               "B":true,
               "R":{"$ref":"#/components/schemas/S","description":"d"}}}}
            """, OpenAPI.class);

        Map<String, Schema> schemas = api.getComponents().getSchemas();
        assertEquals(Set.of("string", "null"), schemas.get("S").getTypes());
        assertEquals(Boolean.TRUE, schemas.get("B").getBooleanSchemaValue());
        String written = OpenApiUtils.JSON_MAPPER_31.writeValueAsString(api);
        assertTrue(written.contains("\"B\":true"), written);
        assertTrue(written.contains("\"$ref\":\"#/components/schemas/S\""), written);
        assertTrue(written.contains("\"description\":\"d\""), written);
    }

    @Test
    void createsSchemasForPrimitiveTypes() {
        for (PrimitiveType type : PrimitiveType.values()) {
            assertNotNull(type.createProperty(), type.name());
            assertNotNull(type.createProperty31(), type.name());
            assertNotNull(type.createProperty(true), type.name());
            assertNotNull(type.getKeyClass(), type.name());
            if (type.getCommonName() != null) {
                assertNotNull(PrimitiveType.fromName(type.getCommonName()), type.name());
            }
        }
        assertEquals(PrimitiveType.STRING, PrimitiveType.fromType(String.class));
        assertEquals(PrimitiveType.UUID, PrimitiveType.fromType(UUID.class));
        assertEquals(PrimitiveType.LONG, PrimitiveType.fromTypeAndFormat("integer", "int64"));
        assertEquals("string", PrimitiveType.getCommonName(String.class));
        assertNotNull(PrimitiveType.createProperty(Date.class));
        assertNotNull(PrimitiveType.createProperty(Date.class, true));
        assertNotNull(PrimitiveType.createProperty("integer"));
        assertNotNull(PrimitiveType.createProperty("integer", true));
        assertNull(PrimitiveType.fromName("not-a-type"));
        assertNull(PrimitiveType.fromType(SwaggerCoreJacksonTest.class));
        assertFalse(PrimitiveType.systemPrefixes().isEmpty());
        assertNotNull(PrimitiveType.customExcludedClasses());
        assertNotNull(PrimitiveType.customExcludedExternalClasses());
        assertNotNull(PrimitiveType.customClasses());
        assertNotNull(PrimitiveType.nonSystemTypes());
        assertNotNull(PrimitiveType.nonSystemTypePackages());
    }
}
