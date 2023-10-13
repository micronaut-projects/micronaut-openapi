package io.micronaut.openapi.test.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.multipart.MultipartBody;
import io.micronaut.openapi.test.model.Animal;
import io.micronaut.openapi.test.model.Bird;
import io.micronaut.openapi.test.model.ColorEnum;
import io.micronaut.openapi.test.model.DateModel;
import io.micronaut.openapi.test.model.Mammal;
import io.micronaut.openapi.test.model.ModelWithEnumList;
import io.micronaut.openapi.test.model.ModelWithInnerEnum;
import io.micronaut.openapi.test.model.ModelWithMapProperty;
import io.micronaut.openapi.test.model.ModelWithRequiredProperties;
import io.micronaut.openapi.test.model.NestedModel;
import io.micronaut.openapi.test.model.Reptile;
import io.micronaut.openapi.test.model.SimpleModel;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@MicronautTest
public class RequestBodyControllerTest {

    @Inject
    EmbeddedServer server;

    @Inject
    @Client("/api")
    HttpClient reactiveClient;

    BlockingHttpClient client;

    @BeforeEach
    void setup() {
        client = reactiveClient.toBlocking();
    }

    @Disabled("Not yet supported")
    @Test
    void testSendValidatedCollection() {
        HttpRequest<?> request =
            HttpRequest.POST("/sendValidatedCollection", List.of(List.of("a", "hello", "123")))
                .contentType(MediaType.APPLICATION_JSON_TYPE);

        var e = assertThrows(HttpClientResponseException.class, () ->
            client.retrieve(request, Argument.of(String.class), Argument.of(String.class)));

        assertEquals(e.getStatus(), HttpStatus.BAD_REQUEST);
        assertTrue(e.getMessage().contains("collection[0][0]: size must be between 3 and 2147483647"));
    }

    @Test
    void testSendSimpleModel() {
        var model = new SimpleModel();
        model.setColor("red");
        model.setNumEdges(10L);
        model.setArea(11.5f);
        model.setConvex(true);
        model.setPoints(List.of("1,1", "2,2", "1,2"));
        HttpRequest<?> request = HttpRequest.POST("/sendSimpleModel", model)
            .contentType(MediaType.APPLICATION_JSON_TYPE);

        var arg = Argument.of(SimpleModel.class);
        SimpleModel response = client.retrieve(request, arg, Argument.of(String.class));

        assertEquals(model, response);
    }

    static Stream<Arguments> models() {
        return Stream.of(
            arguments(new SimpleModel("1", null, null, null, null, null, null), "simpleModel.color: size must be between 2 and 2147483647"),
            arguments(new SimpleModel(null, 0L, null, null, null, null, null), "simpleModel.numEdges: must be greater than or equal to 1"),
            arguments(new SimpleModel(null, null, 0f, null, null, null, null), "simpleModel.area: must be greater than or equal to 0"),
            arguments(new SimpleModel(null, null, null, null, null, List.of("0,0"), null), "simpleModel.points: size must be between 3 and 2147483647")
        );
    }

    @Disabled
    @MethodSource("models")
    @ParameterizedTest
    void testSendValidatedSimpleModel(SimpleModel model, String message) {
        HttpRequest<?> request = HttpRequest.POST("/sendSimpleModel", model);
        var arg = Argument.of(SimpleModel.class);
        client.retrieve(request, arg, Argument.of(String.class));

        var e = assertThrows(HttpClientResponseException.class, () ->
            client.retrieve(request, Argument.of(String.class), Argument.of(String.class)));

        assertEquals(e.getStatus(), HttpStatus.BAD_REQUEST);
        assertTrue(e.getMessage().contains(message));
    }

    @Test
    void testSendListOfSimpleModels() {

        var models = List.of(
            new SimpleModel("red", 10L, 11.5f, null, true, List.of("1,0", "0,0", "0,1", "2,2"), null),
            new SimpleModel("azure", 2L, 1.45f, null, true, List.of("1,1", "2,2"), null),
            new SimpleModel(null, 11L, null, null, false, null, null)
        );

        HttpRequest<?> request = HttpRequest.POST("/sendListOfSimpleModels", models)
            .contentType(MediaType.APPLICATION_JSON_TYPE);
        var arg = Argument.listOf(SimpleModel.class);
        List<SimpleModel> response = client.retrieve(request, arg, Argument.of(String.class));

        assertEquals(models, response);
    }

    @Test
    void testSendModelsWithRequiredPropertiesRequest() {
        var model = new ModelWithRequiredProperties("Walaby", 1.2f, null, null);
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithRequiredProperties", model);

        var arg = Argument.of(ModelWithRequiredProperties.class);
        var response = client.retrieve(request, arg, Argument.of(String.class));

        assertEquals(model, response);
    }

    @Test
    void testSendDateModel() {
        var dateModel = new DateModel(LocalDate.parse("2022-01-03"),
            OffsetDateTime.parse("1999-01-01T00:01:10.456+01:00").toZonedDateTime());
        HttpRequest<?> request = HttpRequest.POST("/sendDateModel", dateModel);

        String response = client.retrieve(request, Argument.of(String.class), Argument.of(String.class));

        assertEquals("""
                    {"commitDate":"2022-01-03","commitDateTime":"1999-01-01T00:01:10.456+01:00"}""", response);
    }

    @Test
    void testSendNestedModel() {

        var simpleModel = new SimpleModel("red", 10L, 11.5f, null, true, List.of("1,1", "2,2", "1,2"), null);
        var model = new NestedModel(simpleModel, null);
        HttpRequest<?> request = HttpRequest.POST("/sendNestedModel", model)
            .contentType(MediaType.APPLICATION_JSON_TYPE);

        Argument<NestedModel> arg = Argument.of(NestedModel.class);
        NestedModel response = client.retrieve(request, arg, Argument.of(String.class));

        assertEquals(model, response);
    }

    @Test
    void testSendModelWithInnerEnum() {

        var model = new ModelWithInnerEnum("Short-eared rock wallaby",40000L,ModelWithInnerEnum.MammalOrder.MARSUPIAL);
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithInnerEnum", model)
            .contentType(MediaType.APPLICATION_JSON_TYPE);

        String response = client.retrieve(request, Argument.of(String.class), Argument.of(String.class));

        assertEquals("""
            {"species-name":"Short-eared rock wallaby","num-representatives":40000,"mammal-order":"marsupial"}""", response);
    }

    @Test
    void testSendModelWithEnumList() {

        var colors = List.of(ColorEnum.DARK_GREEN, ColorEnum.LIGHT_BLUE);
        var model = new ModelWithEnumList(colors);
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithEnumList", model)
            .contentType(MediaType.APPLICATION_JSON_TYPE);

        String response = client.retrieve(request, Argument.of(String.class), Argument.of(String.class));

        assertEquals("""
            {"favoriteColors":["dark-green","light-blue"]}""", response);
    }

    @EnumSource(ColorEnum.class)
    @ParameterizedTest
    void testSendEnumColor(ColorEnum color) {
        HttpRequest<?> request = HttpRequest.POST("/sendEnum", color);
        ColorEnum response = client.retrieve(request, Argument.of(ColorEnum.class));

        assertEquals(color, response);

        String stringResponse = client.retrieve(request, Argument.of(String.class));

        assertEquals("\"" + color.getValue() + "\"", stringResponse);
    }

    @Test
    void testSendEnumList() {

        List<ColorEnum> colors = List.of(ColorEnum.GREEN, ColorEnum.RED);
        HttpRequest<?> request = HttpRequest.POST("/sendEnumList", colors);

        String response = client.retrieve(request, Argument.of(String.class));

        assertEquals("""
            ["green","red"]""", response);
    }

    @Test
    void testSendModelWithSimpleMapProperty() {

        var model = new ModelWithMapProperty();
        model.setMap(Map.of("color", "pink", "weight", "30.4"));
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithMapProperty", model);

        var response = client.retrieve(request, ModelWithMapProperty.class);

        assertEquals(model, response);
    }

    @Test
    void testSendModelWithDeepMapProperty() {

        Map<String, Map<String, String>> map = Map.of(
            "characteristics", Map.of("color", "pink"),
            "issues", Map.of("isWorking", "false", "hasCracks", "true")
        );
        var model = new ModelWithMapProperty();
        model.setDeepMap(map);
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithMapProperty", model);

        var response = client.retrieve(request, ModelWithMapProperty.class);

        assertEquals(model, response);
    }

    @Test
    void testSendModelWithDeepMapModelProperty() {

        var map = Map.of(
        "polygons", Map.of(
        "triangle", new SimpleModel(null, 3L, null, null, null, null, null),
        "smallRectangle", new SimpleModel(null, 4L, 1f, null, null, null, null)
            )
        );
        var model = new ModelWithMapProperty();
        model.setDeepObjectMap(map);
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithMapProperty", model);

        var response = client.retrieve(request, ModelWithMapProperty.class);

        assertEquals(model, response);
    }

    private static final String BIRD_DISCRIMINATOR = "ave";
    private static final String MAMMAL_DISCRIMINATOR = "mammalia";
    private static final String REPTILE_DISCRIMINATOR = "reptilia";

    static Stream<Arguments> discriminators() {
        var bird = new Bird(2, BigDecimal.valueOf(12, 1), "Large blue and white feathers");
        bird.setColor(ColorEnum.BLUE);
        var mammal = new Mammal(20.5f, "A typical Canadian beaver");
        mammal.setColor(ColorEnum.BLUE);
        var reptile = new Reptile(0, true, "A pair of venomous fangs");
        reptile.setColor(ColorEnum.BLUE);
        return Stream.of(
            arguments(BIRD_DISCRIMINATOR, bird),
            arguments(MAMMAL_DISCRIMINATOR, mammal),
            arguments(REPTILE_DISCRIMINATOR, reptile)
        );
    }

    @MethodSource("discriminators")
    @ParameterizedTest
    void testSendModelWithDiscriminatorChild(String discriminatorName, Animal model) {
        HttpRequest<?> request = HttpRequest.PUT("/sendModelWithDiscriminator", model);

        var arg = Argument.of(Animal.class);
        Animal response = client.retrieve(request, arg, Argument.of(String.class));

        assertEquals(model, response);

        String stringResponse = client.retrieve(request, Argument.of(String.class));

        assertTrue(stringResponse.contains("\"class\":\"" + discriminatorName + "\""));
    }

    @Test
    void testSendBytes() {

        String content = "my small bytes content";
        HttpRequest<?> request = HttpRequest.PUT("/sendBytes", content.getBytes())
            .contentType(MediaType.APPLICATION_OCTET_STREAM_TYPE);

        var arg = Argument.of(byte[].class);
        byte[] response = client.retrieve(request, arg, Argument.of(String.class));

        assertEquals(content, new String(response));
    }

    @Test
    void testSendFile() {

        String content = "my favorite file content";
        var body = MultipartBody.builder()
            .addPart("file", "my-file.txt", content.getBytes())
            .build();
        HttpRequest<?> request = HttpRequest.PUT("/sendFile", body)
            .contentType(MediaType.MULTIPART_FORM_DATA_TYPE);

        var arg = Argument.of(byte[].class);
        byte[] response = client.retrieve(request, arg, Argument.of(String.class));

        assertEquals("name: my-file.txt, content: my favorite file content", new String(response));
    }
}
