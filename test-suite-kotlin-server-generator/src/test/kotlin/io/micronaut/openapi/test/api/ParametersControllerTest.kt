package io.micronaut.openapi.test.api

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.openapi.test.model.ColorEnum
import io.micronaut.openapi.test.model.SendPrimitivesResponse
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.stream.Stream
import kotlin.String

@MicronautTest
class ParametersControllerTest(
    var server: EmbeddedServer,
    @Client("/api")
    var reactiveClient: HttpClient,
) {

    lateinit var client: BlockingHttpClient

    @BeforeEach
    fun setup() {
        client = reactiveClient.toBlocking()
    }

    @Test
    fun testSendPrimitives() {
        val request = HttpRequest.GET<Any>("/sendPrimitives/Andrew?age=1&isPositive=true")
                .header("height", "17.3")

        val (name, age, height, isPositive) = client.retrieve(request, Argument.of(SendPrimitivesResponse::class.java), Argument.of(String::class.java))
        assertEquals(name, "Andrew")
        assertEquals(age, BigDecimal.valueOf(1))
        assertEquals(height, 17.3f)
        assertEquals(isPositive, true)
    }

    @Test
    fun testSendNotNullPrimitives() {
        val request = HttpRequest.GET<Any>("/sendPrimitives/Andrew")
        val arg = Argument.of(SendPrimitivesResponse::class.java)
        val e = assertThrows(HttpClientResponseException::class.java) { client.retrieve(request, arg, Argument.of(String::class.java)) }
        assertEquals(e.status, HttpStatus.BAD_REQUEST)
        assertTrue(e.message!!.contains("not specified"))
    }

    @Test
    fun testSendNullablePrimitives() {
        val request = HttpRequest.GET<Any>("/sendValidatedPrimitives")
                .accept(MediaType.TEXT_PLAIN_TYPE)
        client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java))
        assertDoesNotThrow { client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java)) }
    }

    @Test
    fun testSendValidatedPrimitives() {
        val request = HttpRequest.GET<Any>("/sendValidatedPrimitives?name=Andrew&age=20&favoriteNumber=3.14&height=1.5")
        assertDoesNotThrow { client.retrieve(request, Argument.of(String::class.java)) }
    }

    @MethodSource("queries")
    @ParameterizedTest
    fun testSendInvalidPrimitivesMessage(query: String, message: String) {
        val request = HttpRequest.GET<Any>("/sendValidatedPrimitives?$query")
        val e = assertThrows(HttpClientResponseException::class.java) { client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java)) }
        assertEquals(e.status, HttpStatus.BAD_REQUEST)
        assertTrue(e.message!!.contains(message))
    }

    @EnumSource(ColorEnum::class)
    @ParameterizedTest
    fun testSendParameterEnumColor(color: ColorEnum) {
        val request = HttpRequest.GET<Any>("/sendParameterEnum?colorParam=" + color.value)
        val response = client.retrieve(request, Argument.of(ColorEnum::class.java))
        assertEquals(color, response)
        val stringResponse = client.retrieve(request, Argument.of(String::class.java))
        assertEquals('"'.toString() + color.value + '"', stringResponse)
    }

    @Test
    fun testSendDates() {
        val request = HttpRequest.GET<Any>("/sendDates?commitDate=2022-03-04&commitDateTime=2022-03-04T12:00:01.321Z")
        val response = client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java))
        assertEquals("""
            {"commitDate":"2022-03-04","commitDateTime":"2022-03-04T12:00:01.321Z"}
            """.trimIndent(), response)
    }

    @Test
    fun testSendPageQuery() {
        val request = HttpRequest.GET<Any>("/sendPageQuery?page=2&size=20&sort=my-property,desc&sort=my-property-2")
        val response = client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java))
        assertEquals("(page: 2, size: 20, sort: my-property(dir=DESC) my-property-2(dir=ASC))", response)
    }

    @Test
    fun testSendMappedParameter() {
        val filter = "name=Andrew,age>20"
        val request = HttpRequest.GET<Any>("/sendMappedParameter")
                .header("Filter", filter)
        val response = client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java))
        assertEquals(response, filter)
    }

    @Test
    fun testSendInvalidMappedParameter() {
        val request = HttpRequest.GET<Any>("/sendMappedParameter")
                .header("Filter", "name=Andrew,age>20,something-else")
        val e = assertThrows(HttpClientResponseException::class.java) { client.retrieve(request, Argument.of(String::class.java), Argument.of(String::class.java)) }
        assertEquals(HttpStatus.BAD_REQUEST, e.status)
    }

    companion object {
        @JvmStatic
        fun queries(): Stream<Arguments> {
            return Stream.of(
                    arguments("name=aa", "name: size must be between 3 and 2147483647"),
                    arguments("name=123456", "name: must match \\\"[a-zA-Z]+\\\""),
                    arguments("age=-2", "age: must be greater than or equal to 10"),
                    arguments("age=500", "age: must be less than or equal to 200"),
                    arguments("favoriteNumber=-500", "favoriteNumber: must be greater than or equal to -100.5"),
                    arguments("favoriteNumber=100.6", "favoriteNumber: must be less than or equal to 100.5"),
                    arguments("height=0", "height: must be greater than or equal to 0.1"),
                    arguments("height=100", "height: must be less than or equal to 3")
            )
        }
    }
}
