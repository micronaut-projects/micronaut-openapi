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
import io.micronaut.openapi.test.model.*
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.stream.Stream

@MicronautTest
class RequestBodyControllerTest(
        var server: EmbeddedServer,
        @Client("/api")
        var reactiveClient: HttpClient,
) {

    lateinit var client: BlockingHttpClient

    @BeforeEach
    fun setup() {
        client = reactiveClient.toBlocking()
    }

    @Disabled("Not yet supported")
    @Test
    fun testSendValidatedCollection() {
        val request = HttpRequest.POST("/sendValidatedCollection", listOf(listOf("a", "hello", "123")))
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        val e = assertThrows(HttpClientResponseException::class.java) {
            client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java))
        }
        assertEquals(e.status, HttpStatus.BAD_REQUEST)
        assertTrue(e.message!!.contains("requestBody[0][0]: size must be between 3 and 2147483647"))
    }

    @Test
    fun testSendSimpleModel() {
        val model = SimpleModel("red", 10L, 11.5F, null, true, listOf("1,1", "2,2", "1,2"))
        val request = HttpRequest.POST("/sendSimpleModel", model)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        val response = client.retrieve(request, Argument.of(SimpleModel::class.java), Argument.of(String::class.java))
        assertEquals(model, response)
    }

    @MethodSource("models")
    @ParameterizedTest
    fun testSendValidatedSimpleModel(model: SimpleModel, message: String?) {
        val request = HttpRequest.POST("/sendSimpleModel", model)
        val e = assertThrows(HttpClientResponseException::class.java) {
            client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java))
        }
        assertEquals(e.status, HttpStatus.BAD_REQUEST)
        assertTrue(e.message!!.contains(message!!))
    }

    @Test
    fun testSendListOfSimpleModels() {
        val models = listOf(
                SimpleModel(color = "red", numEdges = 10L, area = 11.5f, convex = true, points = listOf("1,0", "0,0", "0,1", "2,2")),
                SimpleModel(color = "azure", numEdges = 2L, area = 1.45f, convex = true, points = listOf("1,1", "2,2")),
                SimpleModel(numEdges = 11L, convex = false)
        )
        val request = HttpRequest.POST("/sendListOfSimpleModels", models)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        val response = client.retrieve(request, Argument.listOf(SimpleModel::class.java), Argument.of(String::class.java))
        assertEquals(models, response)
    }

    @Test
    fun testSendModelsWithRequiredPropertiesRequest() {
        val model = ModelWithRequiredProperties("Walaby", 1.2f, null, null)
        val request = HttpRequest.POST("/sendModelWithRequiredProperties", model)
        val response = client.retrieve(request, Argument.of(ModelWithRequiredProperties::class.java), Argument.of(String::class.java))
        assertEquals(model, response)
    }

    @Test
    fun testSendDateModel() {
        val dateModel = DateModel(LocalDate.parse("2022-01-03"),
                OffsetDateTime.parse("1999-01-01T00:01:10.456+01:00").toZonedDateTime())
        val request = HttpRequest.POST("/sendDateModel", dateModel)
        val response = client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java))
        assertEquals("""{"commitDate":"2022-01-03","commitDateTime":"1999-01-01T00:01:10.456+01:00"}""", response)
    }

    @Test
    fun testSendNestedModel() {
        val simpleModel = SimpleModel("red", 10L, 11.5f, null, true, listOf("1,1", "2,2", "1,2"), null)
        val model = NestedModel(simpleModel, null)
        val request = HttpRequest.POST("/sendNestedModel", model)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        val response = client.retrieve(request, Argument.of(NestedModel::class.java), Argument.of(String::class.java))
        assertEquals(model, response)
    }

    @Test
    fun testSendModelWithInnerEnum() {
        val model = ModelWithInnerEnum("Short-eared rock wallaby", 40000L, ModelWithInnerEnum.MammalOrder.MARSUPIAL)
        val request = HttpRequest.POST("/sendModelWithInnerEnum", model)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        val response = client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java))
        assertEquals("""{"species-name":"Short-eared rock wallaby","num-representatives":40000,"mammal-order":"marsupial"}""", response)
    }

    @Test
    fun testSendModelWithEnumList() {
        val colors = listOf(ColorEnum.DARK_GREEN, ColorEnum.LIGHT_BLUE)
        val model = ModelWithEnumList(colors)
        val request = HttpRequest.POST("/sendModelWithEnumList", model)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        val response = client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java))
        assertEquals("""{"favoriteColors":["dark-green","light-blue"]}""", response)
    }

    @EnumSource(ColorEnum::class)
    @ParameterizedTest
    fun testSendEnumColor(color: ColorEnum) {
        val request = HttpRequest.POST("/sendEnum", color)
        val response = client.retrieve(request, Argument.of(ColorEnum::class.java))
        assertEquals(color, response)
        val stringResponse = client.retrieve(request, Argument.of(String::class.java))
        assertEquals(""""${color.value}"""", stringResponse)
    }

    @Test
    fun testSendEnumList() {
        val colors = listOf(ColorEnum.GREEN, ColorEnum.RED)
        val request = HttpRequest.POST("/sendEnumList", colors)
        val response = client.retrieve(request, Argument.of(String::class.java))
        assertEquals("""["green","red"]""", response)
    }

    @Test
    fun testSendModelWithSimpleMapProperty() {
        val model = ModelWithMapProperty()
        model.map = mapOf("color" to "pink", "weight" to "30.4")
        val request = HttpRequest.POST("/sendModelWithMapProperty", model)
        val response = client.retrieve(request, ModelWithMapProperty::class.java)
        assertEquals(model, response)
    }

    @Disabled
    @MethodSource("models2")
    @ParameterizedTest
    fun testSendModelWithValidatedListProperty(model: ModelWithValidatedListProperty, messageContent: String?) {
        val request = HttpRequest.POST("/sendModelWithValidatedListProperty", model)
        val e = assertThrows(HttpClientResponseException::class.java) { client.retrieve(request, String::class.java) }
        assertTrue(e.response.body().toString().contains(messageContent!!))
    }

    @Test
    fun testSendModelWithDeepMapProperty() {
        val model = ModelWithMapProperty(deepMap = mapOf(
                "characteristics" to mapOf("color" to "pink"),
                "issues" to mapOf("isWorking" to "false", "hasCracks" to "true")
        ))
        val request = HttpRequest.POST("/sendModelWithMapProperty", model)
        val response = client.retrieve(request, ModelWithMapProperty::class.java)
        assertEquals(model, response)
    }

    @Disabled
    @MethodSource("models3")
    @ParameterizedTest
    fun testSendModelWithValidatedDeepMapProperty(model: ModelWithMapProperty, messageContent: String) {
        val request = HttpRequest.POST("/sendModelWithMapProperty", model)
        val e = assertThrows(HttpClientResponseException::class.java) { client.retrieve(request, String::class.java) }
        assertTrue(e.response.body().toString().contains(messageContent))
    }

    @Test
    fun testSendModelWithDeepMapModelProperty() {
        val model = ModelWithMapProperty(deepObjectMap = mapOf(
                "polygons" to mapOf(
                        "triangle" to SimpleModel(numEdges = 3L),
                        "smallRectangle" to SimpleModel(numEdges = 4L, area = 1F)
                )
        ))
        val request = HttpRequest.POST("/sendModelWithMapProperty", model)
        val response = client.retrieve(request, ModelWithMapProperty::class.java)
        assertEquals(model, response)
    }

    @MethodSource("discriminators")
    @ParameterizedTest
    fun testSendModelWithDiscriminatorChild(discriminatorName: String, model: Animal) {
        val request = HttpRequest.PUT("/sendModelWithDiscriminator", model)
        val response = client.retrieve(request, Argument.of(Animal::class.java), Argument.of(String::class.java))

        assertEquals(discriminatorName, response.propertyClass)

        response.propertyClass = null
        assertEquals(model, response)

        val stringResponse = client.retrieve(request, Argument.of(String::class.java))
        assertTrue(stringResponse.contains(""""class":"$discriminatorName""""))
    }

    @Test
    fun testSendBytes() {
        val content = "my small bytes content"
        val request = HttpRequest.PUT("/sendBytes", content.toByteArray())
                .contentType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
        val arg = Argument.of(ByteArray::class.java)
        val response = client.retrieve(request, arg, Argument.of(String::class.java))
        assertEquals(content, String(response))
    }

    @Test
    fun testSendFile() {
        val content = "my favorite file content"
        val body = MultipartBody.builder()
                .addPart("file", "my-file.txt", content.toByteArray())
                .build()
        val request = HttpRequest.PUT("/sendFile", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
        val arg = Argument.of(ByteArray::class.java)
        val response = client.retrieve(request, arg, Argument.of(String::class.java))
        assertEquals("name: my-file.txt, content: my favorite file content", String(response))
    }

    companion object {
        @JvmStatic
        fun models(): Stream<Arguments> =
                Stream.of(
                        arguments(SimpleModel(color = "1"), "simpleModel.color: size must be between 2 and 2147483647"),
                        arguments(SimpleModel(numEdges = 0L), "simpleModel.numEdges: must be greater than or equal to 1"),
                        arguments(SimpleModel(area = 0f), "simpleModel.area: must be greater than or equal to 0"),
                        arguments(SimpleModel(points = listOf("0,0")), "simpleModel.points: size must be between 3 and 2147483647")
                )

        @JvmStatic
        fun models2(): Stream<Arguments> =
                Stream.of(
                        arguments(ModelWithValidatedListProperty(stringList = listOf("one", "two", "")), "modelWithValidatedListProperty.stringList[2]: size must be between 3 and 2147483647"),
                        arguments(ModelWithValidatedListProperty(objectList = listOf(SimpleModel(), SimpleModel(numEdges = 0L))), "modelWithValidatedListProperty.objectList[1].numEdges: must be greater than or equal to 1"),
                        arguments(ModelWithValidatedListProperty(objectList = listOf(SimpleModel(), SimpleModel(), SimpleModel())), "modelWithValidatedListProperty.objectList: size must be between 0 and 2")
                )

        @JvmStatic
        fun models3(): Stream<Arguments> =
                Stream.of(
                        arguments(ModelWithMapProperty(deepMap = mapOf("first" to mapOf("second" to "aa", "third" to "a"))), "model.deepMap[first][third]: size must be between 2 and 2147483647"),
                        arguments(ModelWithMapProperty(deepObjectMap = mapOf("first" to mapOf("second" to SimpleModel(color = "a")))), "model.deepObjectMap[first][second].color: size must be between 2 and 2147483647")
                )

        @JvmStatic
        fun discriminators(): Stream<Arguments> {
            val bird = Bird(2, BigDecimal.valueOf(12, 1), "Large blue and white feathers")
            bird.color = ColorEnum.BLUE
            val mammal = Mammal(20.5f, "A typical Canadian beaver")
            mammal.color = ColorEnum.BLUE
            val reptile = Reptile(0, true, "A pair of venomous fangs")
            reptile.color = ColorEnum.BLUE
            return Stream.of(
                    arguments("ave", bird),
                    arguments("mammalia", mammal),
                    arguments("reptilia", reptile)
            )
        }
    }
}
