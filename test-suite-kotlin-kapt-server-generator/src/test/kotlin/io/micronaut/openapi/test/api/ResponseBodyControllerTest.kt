package io.micronaut.openapi.test.api

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.openapi.test.model.DateModel
import io.micronaut.openapi.test.model.SimpleModel
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@MicronautTest
class ResponseBodyControllerTest(
    val server: EmbeddedServer,
    @Client("/api")
    val reactiveClient: HttpClient,
) {

    lateinit var client: BlockingHttpClient

    @BeforeEach
    fun setup() {
        client = reactiveClient.toBlocking()
    }

    @Test
    fun testGetSimpleModel() {
        val response = client.exchange("/getSimpleModel", SimpleModel::class.java)
        assertEquals(HttpStatus.OK, response.status())
        assertEquals(ResponseBodyController.SIMPLE_MODEL, response.body())
    }

    @Test
    fun testGetDateTime() {
        val response = client.exchange("/getDateTime", ZonedDateTime::class.java)
        assertEquals(HttpStatus.OK, response.status())
        assertEquals(ResponseBodyController.DATE_TIME_INSTANCE, response.body())
        val stringResponse = client.retrieve("/getDateTime", String::class.java)
        assertEquals("\"2022-12-04T06:35:00.784-05:00[America/Toronto]\"", stringResponse)
    }

    @Test
    fun testGetDateModel() {
        val response = client.exchange("/getDateModel", DateModel::class.java)
        assertEquals(HttpStatus.OK, response.status())
        assertEquals(ResponseBodyController.DATE_MODEL_INSTANCE, response.body())
        val strResponse = client.retrieve("/getDateModel", String::class.java)
        assertEquals("""{"commitDate":"2023-06-27","commitDateTime":"2022-12-04T06:35:00.784-05:00[America/Toronto]"}""", strResponse)
    }

    @Test
    fun testGetPaginatedSimpleModel() {
        val page = "12"
        val pageSize = "10"
        val request = HttpRequest.GET<Any>("/getPaginatedSimpleModel?page=$page&size=$pageSize")
        val response = client.exchange(request, Argument.listOf(SimpleModel::class.java))
        val totalCount = "3"
        val pageCount = "1"
        assertEquals(HttpStatus.OK, response.status())
        assertEquals(ResponseBodyController.SIMPLE_MODELS, response.body())
        assertEquals(page, response.header("X-Page-Number"))
        assertEquals(totalCount, response.header("X-Total-Count"))
        assertEquals(3, response.body().size)
        assertEquals(pageSize, response.header("X-Page-Size"))
        assertEquals(pageCount, response.header("X-Page-Count"))
    }

    @Test
    fun testGetDatedSimpleModel() {
        val response = client.exchange(HttpRequest.GET<Any>("/getDatedSimpleModel"), Argument.of(SimpleModel::class.java))
        assertEquals(HttpStatus.OK, response.status())
        assertEquals(ResponseBodyController.SIMPLE_MODEL, response.body())
        assertEquals(ResponseBodyController.LAST_MODIFIED_STRING, response.header("Last-Modified"))
    }

    @Test
    fun testGetSimpleModelWithNonStandardStatus() {
        val response = client.exchange("/getSimpleModelWithNonStandardStatus", SimpleModel::class.java)
        assertEquals(HttpStatus.CREATED, response.status())
        assertEquals(ResponseBodyController.SIMPLE_MODEL, response.body())
    }

    @Test
    fun testGetDatedSimpleModelWithNonMappedHeader() {
        val response = client.exchange(
                HttpRequest.GET<Any>("/getDatedSimpleModelWithNonMappedHeader"), Argument.of(SimpleModel::class.java))
        assertEquals(HttpStatus.OK, response.status())
        assertEquals(ResponseBodyController.SIMPLE_MODEL, response.body())
        assertEquals("custom-value", response.header("custom-header"))
        assertEquals(ResponseBodyController.LAST_MODIFIED_STRING, response.header("Last-Modified"))
    }

    @Test
    fun testGetSimpleModelWithNonMappedHeader() {
        val response = client.exchange("/getSimpleModelWithNonMappedHeader", SimpleModel::class.java)
        assertEquals(HttpStatus.OK, response.status())
        assertEquals("custom-value-2", response.headers["custom-header"])
        assertEquals(ResponseBodyController.SIMPLE_MODEL, response.body())
    }

    @Test
    fun testGetErrorResponse() {
        val e = Assertions.assertThrows(HttpClientResponseException::class.java) { client.retrieve(HttpRequest.GET<Any>("/getErrorResponse"), Argument.of(String::class.java), Argument.of(String::class.java)) }
        assertEquals(e.status, HttpStatus.NOT_FOUND)
    }

    @Disabled("https://github.com/micronaut-projects/micronaut-core/issues/10733")
    @Test
    fun testGetFile() {
        val request = HttpRequest.GET<Any>("/getFile")
        val arg = Argument.of(ByteArray::class.java)
        val response = client.retrieve(request, arg, Argument.of(String::class.java))
        val expectedContent = "My file content"
        assertEquals(expectedContent, String(response))
    }
}
