package io.micronaut.openapi.test.api

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.openapi.test.model.Animal
import io.micronaut.openapi.test.model.Bird
import io.micronaut.openapi.test.model.ColorEnum
import io.micronaut.openapi.test.model.DateModel
import io.micronaut.openapi.test.model.Mammal
import io.micronaut.openapi.test.model.ModelWithEnumList
import io.micronaut.openapi.test.model.ModelWithInnerEnum
import io.micronaut.openapi.test.model.ModelWithMapProperty
import io.micronaut.openapi.test.model.ModelWithRequiredProperties
import io.micronaut.openapi.test.model.NestedModel
import io.micronaut.openapi.test.model.Reptile
import io.micronaut.openapi.test.model.SimpleModel
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Ignore
import spock.lang.Specification

import java.time.LocalDate
import java.time.OffsetDateTime

@MicronautTest
class RequestBodyControllerSpec extends Specification {

    @Inject
    EmbeddedServer server

    @Inject
    @Client("/api")
    HttpClient reactiveClient

    BlockingHttpClient client

    void setup() {
        this.client = reactiveClient.toBlocking()
    }

    @Ignore("Not yet supported")
    void "test send validated collection"() {
        given:
        HttpRequest<?> request =
                HttpRequest.POST("/sendValidatedCollection", List.of(List.of("a", "hello", "123")))
                        .contentType(MediaType.APPLICATION_JSON_TYPE)

        when:
        client.retrieve(request, Argument.of(String), Argument.of(String))

        then:
        def e = thrown(HttpClientResponseException)

        HttpStatus.BAD_REQUEST == e.status
        e.message.contains("collection[0][0]: size must be between 3 and 2147483647")
    }

    void "test send simple model"() {
        given:
        SimpleModel model =
                new SimpleModel().color("red").numEdges(10L).area(11.5f)
                        .convex(true).points(List.of("1,1", "2,2", "1,2"))
        HttpRequest<?> request = HttpRequest.POST("/sendSimpleModel", model)
                .contentType(MediaType.APPLICATION_JSON_TYPE)

        when:
        Argument<SimpleModel> arg = Argument.of(SimpleModel)
        SimpleModel response = client.retrieve(request, arg, Argument.of(String))

        then:
        model == response
    }

    void "test send validated simple model: #message"() {
        given:
        HttpRequest<?> request = HttpRequest.POST("/sendSimpleModel", model)

        when:
        Argument<SimpleModel> arg = Argument.of(SimpleModel)
        client.retrieve(request, arg, Argument.of(String))

        then:
        def e = thrown(HttpClientResponseException)
        HttpStatus.BAD_REQUEST == e.status
        e.message != null
        e.message.contains(message)

        where:
        model                             | message
        new SimpleModel().color("1")      | "simpleModel.color: size must be between 2 and 2147483647"
        new SimpleModel().numEdges(0L)    | "simpleModel.numEdges: must be greater than or equal to 1"
        new SimpleModel().area(0f)        | "simpleModel.area: must be greater than or equal to 0"
        new SimpleModel().points(["0,0"]) | "simpleModel.points: size must be between 3 and 2147483647"
    }

    void "test send list of simple models"() {
        given:
        List<SimpleModel> models = [
                new SimpleModel().color("red").numEdges(10L).area(11.5f)
                                .convex(true).points(["1,0", "0,0", "0,1", "2,2"]),
                new SimpleModel().color("azure").numEdges(2L).area(1.45f)
                                .convex(true).points(["1,1", "2,2"]),
                new SimpleModel().numEdges(11L).convex(false)
        ]

        when:
        HttpRequest<?> request = HttpRequest.POST("/sendListOfSimpleModels", models)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        Argument<List<SimpleModel>> arg = Argument.listOf(SimpleModel)
        List<SimpleModel> response = client.retrieve(request, arg, Argument.of(String))

        then:
        models == response
    }

    void "test send models with required properties request"() {
        given:
        ModelWithRequiredProperties model = new ModelWithRequiredProperties("Walaby", 1.2f)
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithRequiredProperties", model)

        when:
        Argument<ModelWithRequiredProperties> arg = Argument.of(ModelWithRequiredProperties)
        def response = client.retrieve(request, arg, Argument.of(String))

        then:
        model == response
    }

    void "test send model with missing required properties"() {
        given:
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithRequiredProperties", model)

        when:
        Argument<ModelWithRequiredProperties> arg = Argument.of(ModelWithRequiredProperties)
        client.retrieve(request, arg, Argument.of(String))

        then:
        def e = thrown(HttpClientResponseException)
        HttpStatus.BAD_REQUEST == e.status

        where:
        model                                                               | _
        new ModelWithRequiredProperties(null, 1.3f)
                .numRepresentatives(100000).description("A hopping animal") | _
        new ModelWithRequiredProperties("Walaby", null)
                .numRepresentatives(100000).description("A hopping animal") | _
    }

    void "test send date model"() {
        given:
        DateModel dateModel = new DateModel()
                .commitDate(LocalDate.parse("2022-01-03"))
                .commitDateTime(OffsetDateTime.parse("1999-01-01T00:01:10.456+01:00").toZonedDateTime())
        HttpRequest<?> request = HttpRequest.POST("/sendDateModel", dateModel)

        when:
        String response = client.retrieve(request, Argument.of(String), Argument.of(String))

        then:
        '{"commitDate":"2022-01-03","commitDateTime":"1999-01-01T00:01:10.456+01:00"}' == response
    }

    void "test send nested model"() {
        given:
        SimpleModel simpleModel = new SimpleModel()
                .color("red").numEdges(10L).area(11.5f)
                .convex(true).points(List.of("1,1", "2,2", "1,2"))
        NestedModel model = new NestedModel().simpleModel(simpleModel)
        HttpRequest<?> request = HttpRequest.POST("/sendNestedModel", model)
                .contentType(MediaType.APPLICATION_JSON_TYPE)

        when:
        Argument<NestedModel> arg = Argument.of(NestedModel)
        NestedModel response = client.retrieve(request, arg, Argument.of(String))

        then:
        model == response
    }

    void "test send model with inner enum"() {
        given:
        ModelWithInnerEnum model = new ModelWithInnerEnum()
                .speciesName("Short-eared rock wallaby")
                .numRepresentatives(40000L)
                .mammalOrder(ModelWithInnerEnum.MammalOrderEnum.MARSUPIAL)
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithInnerEnum", model)
                .contentType(MediaType.APPLICATION_JSON_TYPE)

        when:
        String response = client.retrieve(request, Argument.of(String), Argument.of(String))

        then:
        '{"species-name":"Short-eared rock wallaby","num-representatives":40000,"mammal-order":"marsupial"}' == response
    }

    void "test send model with enum list"() {
        given:
        List<ColorEnum> colors = [ColorEnum.DARK_GREEN, ColorEnum.LIGHT_BLUE]
        ModelWithEnumList model = new ModelWithEnumList().favoriteColors(colors)
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithEnumList", model)
                .contentType(MediaType.APPLICATION_JSON_TYPE)

        when:
        String response = client.retrieve(request, Argument.of(String), Argument.of(String))

        then:
        '{"favoriteColors":["dark-green","light-blue"]}' == response
    }

    void "test send enum #color"() {
        given:
        HttpRequest<?> request = HttpRequest.POST("/sendEnum", color)

        when:
        ColorEnum response = client.retrieve(request, Argument.of(ColorEnum))

        then:
        color == response

        when:
        String stringResponse = client.retrieve(request, Argument.of(String))

        then:
        '"' + color.value + '"' == stringResponse

        where:
        color                | _
        ColorEnum.BLUE       | _
        ColorEnum.RED        | _
        ColorEnum.GREEN      | _
        ColorEnum.LIGHT_BLUE | _
        ColorEnum.DARK_GREEN | _
    }

    void "test send enum list"() {
        given:
        List<ColorEnum> colors = [ColorEnum.GREEN, ColorEnum.RED]
        HttpRequest<?> request = HttpRequest.POST("/sendEnumList", colors)

        when:
        String response = client.retrieve(request, Argument.of(String))

        then:
        '["green","red"]' == response
    }

    void "test send model with simple map property"() {
        given:
        def model = new ModelWithMapProperty().map(["color": "pink", "weight": "30.4"])
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithMapProperty", model)

        when:
        ModelWithMapProperty response = client.retrieve(request, ModelWithMapProperty)

        then:
        model == response
    }

    void "test send model with deep map property"() {
        given:
        Map<String, Map<String, String>> map = [
                "characteristics": ["color": "pink"],
                "issues": ["isWorking": "false", "hasCracks": "true"]
        ]
        ModelWithMapProperty model = new ModelWithMapProperty().deepMap(map)
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithMapProperty", model)

        when:
        ModelWithMapProperty response = client.retrieve(request, ModelWithMapProperty)

        then:
        model == response
    }

    void "test send model with deep map model property"() {
        given:
        def map = [
                "polygons": [
                        "triangle": new SimpleModel().numEdges(3L),
                        "smallRectangle": new SimpleModel().numEdges(4L).area(1f)
                ]
        ]
        ModelWithMapProperty model = new ModelWithMapProperty().deepObjectMap(map)
        HttpRequest<?> request = HttpRequest.POST("/sendModelWithMapProperty", model)

        when:
        ModelWithMapProperty response = client.retrieve(request, ModelWithMapProperty)

        then:
        model == response
    }

    private static String BIRD_DISCRIMINATOR = "ave"
    private static String MAMMAL_DISCRIMINATOR = "mammalia"
    private static String REPTILE_DISCRIMINATOR = "reptilia"

    @Ignore("Requires fixing")
    void "test send model with discriminator child: #discriminatorName"() {
        given:
        HttpRequest<?> request = HttpRequest.PUT("/sendModelWithDiscriminator", model)

        when:
        Argument<Animal> arg = Argument.of(Animal)
        Animal response = client.retrieve(request, arg, Argument.of(String))

        then:
        model == response

        when:
        String stringResponse = client.retrieve(request, Argument.of(String))

        then:
        stringResponse.contains('"class":"' + discriminatorName + '"')

        where:
        discriminatorName | model
        BIRD_DISCRIMINATOR | new Bird().beakLength(BigDecimal.valueOf(12, 1))
                .featherDescription("Large blue and white feathers").numWings(2).color(ColorEnum.BLUE)
        MAMMAL_DISCRIMINATOR | new Mammal(20.5f, "A typical Canadian beaver").color(ColorEnum.BLUE)
        REPTILE_DISCRIMINATOR | new Reptile(0, true)
                .fangDescription("A pair of venomous fangs")
                .color(ColorEnum.BLUE)
    }

    void "test send bytes"() {
        given:
        String content = "my small bytes content"
        HttpRequest<?> request = HttpRequest.PUT("/sendBytes", content.getBytes())
                .contentType(MediaType.APPLICATION_OCTET_STREAM_TYPE)

        when:
        Argument<byte[]> arg = Argument.of(byte[])
        byte[] response = client.retrieve(request, arg, Argument.of(String))

        then:
        content == new String(response)
    }

    void "test send file"() {
        given:
        String content = "my favorite file content"
        MultipartBody body = MultipartBody.builder()
                .addPart("file", "my-file.txt", content.getBytes()).build()
        HttpRequest<?> request = HttpRequest.PUT("/sendFile", body).contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        when:
        Argument<byte[]> arg = Argument.of(byte[])
        byte[] response = client.retrieve(request, arg, Argument.of(String))

        then:
        'name: my-file.txt, content: my favorite file content' == new String(response)
    }

}
