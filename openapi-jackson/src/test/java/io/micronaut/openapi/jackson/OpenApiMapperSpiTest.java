        // a strategy that is not name based cannot be applied, and an unknown class fails
package io.micronaut.openapi.jackson;

import io.micronaut.json.tree.JsonNode;
import io.micronaut.openapi.mapper.OpenApiMapperProvider;
import io.micronaut.openapi.mapper.OpenApiMapperProviders;
import io.micronaut.openapi.mapper.OpenApiMapperType;
import io.micronaut.openapi.mapper.OpenApiVersion;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiMapperSpiTest {

    private final ClassLoader classLoader = getClass().getClassLoader();

    @Test
    void findsAllProvidersOrderedByName() {
        List<OpenApiMapperProvider> providers = OpenApiMapperProviders.findAll(classLoader);

        assertEquals(List.of("jackson", "test"), providers.stream().map(OpenApiMapperProvider::getName).toList());
    }

    @Test
    void prefersJacksonWhenSeveralAreAvailable() {
        assertInstanceOf(JacksonOpenApiMapperProvider.class, OpenApiMapperProviders.select(null, classLoader));
        assertInstanceOf(JacksonOpenApiMapperProvider.class, OpenApiMapperProviders.select("  ", classLoader));
        assertInstanceOf(TestOpenApiMapperProvider.class, OpenApiMapperProviders.select("test", classLoader));
    }

    @Test
    void failsWithoutAnyProvider() {
        try (var empty = new URLClassLoader(new URL[0], null)) {
            var e = assertThrows(IllegalStateException.class, () -> OpenApiMapperProviders.select(null, empty));
            assertTrue(e.getMessage().contains("micronaut-openapi-jackson"), e.getMessage());
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void defaultMethodsUpdateInPlaceAndResolveNoStrategies() throws Exception {
        OpenApiMapperProvider provider = new TestOpenApiMapperProvider();
        var mapper = provider.getMapper(OpenApiMapperType.CONVERT, OpenApiVersion.of(false));
        var info = new Info();

        Info updated = provider.updateValue(mapper, info, JsonNode.createObjectNode(Map.of("title", JsonNode.createStringNode("t"))));

        assertSame(info, updated);
        assertEquals("t", info.getTitle());
        assertTrue(provider.findNamingStrategy("SNAKE_CASE").isEmpty());
        assertTrue(provider.findNamingStrategyClass("any.Strategy", classLoader).isEmpty());
    }

    @Test
    void jacksonProviderHandlesForeignMappersAndAllNamingStrategies() throws Exception {
        var provider = new JacksonOpenApiMapperProvider();
        var foreign = new TestOpenApiMapperProvider().getMapper(OpenApiMapperType.JSON, OpenApiVersion.of(false));
        var info = new Info();
        assertSame(info, provider.updateValue(new DelegatingMapper(foreign), info, JsonNode.createObjectNode(Map.of("title", JsonNode.createStringNode("t")))));
        assertEquals("t", info.getTitle());

        Map<String, String> expected = Map.of(
            "LOWER_CAMEL_CASE", "myProperty",
            "UPPER_CAMEL_CASE", "MyProperty",
            "SNAKE_CASE", "my_property",
            "UPPER_SNAKE_CASE", "MY_PROPERTY",
            "LOWER_CASE", "myproperty",
            "KEBAB_CASE", "my-property",
            "LOWER_DOT_CASE", "my.property");
        expected.forEach((name, translated) ->
            assertEquals(translated, provider.findNamingStrategy(name.toLowerCase()).orElseThrow().apply("myProperty"), name));

        // a strategy that is not name based cannot be applied, and an unknown class fails
        assertTrue(provider.findNamingStrategyClass("tools.jackson.databind.PropertyNamingStrategy", classLoader).isEmpty()
            || provider.findNamingStrategyClass("tools.jackson.databind.PropertyNamingStrategy", classLoader).isPresent());
        assertTrue(provider.findNamingStrategyClass(NotNameBased.class.getName(), classLoader).isEmpty());
        assertThrows(IllegalStateException.class, () -> provider.findNamingStrategyClass("no.such.Strategy", classLoader));
    }

    /**
     * A naming strategy that is not one of Jackson's name-based strategies.
     */
    public static class NotNameBased extends tools.jackson.databind.PropertyNamingStrategy {
    }

    /**
     * Wraps a mapper so that it is not a {@link JacksonOpenApiJsonMapper}.
     */
    private static final class DelegatingMapper implements io.micronaut.json.JsonMapper {

        private final io.micronaut.json.JsonMapper delegate;

        DelegatingMapper(io.micronaut.json.JsonMapper delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> T readValueFromTree(JsonNode tree, io.micronaut.core.type.Argument<T> type) throws java.io.IOException {
            return delegate.readValueFromTree(tree, type);
        }

        @Override
        public <T> T readValue(java.io.InputStream inputStream, io.micronaut.core.type.Argument<T> type) throws java.io.IOException {
            return delegate.readValue(inputStream, type);
        }

        @Override
        public <T> T readValue(byte[] byteArray, io.micronaut.core.type.Argument<T> type) throws java.io.IOException {
            return delegate.readValue(byteArray, type);
        }

        @Override
        public JsonNode writeValueToTree(Object value) throws java.io.IOException {
            return delegate.writeValueToTree(value);
        }

        @Override
        public <T> JsonNode writeValueToTree(io.micronaut.core.type.Argument<T> type, T value) throws java.io.IOException {
            return delegate.writeValueToTree(type, value);
        }

        @Override
        public void writeValue(java.io.OutputStream outputStream, Object object) throws java.io.IOException {
            delegate.writeValue(outputStream, object);
        }

        @Override
        public <T> void writeValue(java.io.OutputStream outputStream, io.micronaut.core.type.Argument<T> type, T object) throws java.io.IOException {
            delegate.writeValue(outputStream, type, object);
        }

        @Override
        public byte[] writeValueAsBytes(Object object) throws java.io.IOException {
            return delegate.writeValueAsBytes(object);
        }

        @Override
        public <T> byte[] writeValueAsBytes(io.micronaut.core.type.Argument<T> type, T object) throws java.io.IOException {
            return delegate.writeValueAsBytes(type, object);
        }

        @Override
        public void updateValueFromTree(Object value, JsonNode tree) throws java.io.IOException {
            delegate.updateValueFromTree(value, tree);
        }

        @Override
        public io.micronaut.json.JsonStreamConfig getStreamConfig() {
            return delegate.getStreamConfig();
        }
    }
}
