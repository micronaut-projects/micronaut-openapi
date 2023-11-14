package io.micronaut.openapi.test.api

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.server.types.files.FileCustomizableResponseType
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.openapi.test.dated.DatedResponse
import io.micronaut.openapi.test.model.DateModel
import io.micronaut.openapi.test.model.ModelWithValidatedListProperty
import io.micronaut.openapi.test.model.SimpleModel
import io.micronaut.openapi.test.model.StateEnum
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Controller
open class ResponseBodyController : ResponseBodyApi {

    override fun getSimpleModel(): Mono<SimpleModel> {
        return Mono.just(SIMPLE_MODEL)
    }

    override fun getDateTime(): Mono<ZonedDateTime> {
        return Mono.just(DATE_TIME_INSTANCE)
    }

    override fun getDateModel(): Mono<DateModel> {
        return Mono.just(DATE_MODEL_INSTANCE)
    }

    override fun getPaginatedSimpleModel(pageable: Pageable): Mono<Page<SimpleModel>> {
        return Mono.just(Page.of(SIMPLE_MODELS, pageable, SIMPLE_MODELS.size.toLong()))
    }

    override fun getDatedSimpleModel(): Mono<DatedResponse<SimpleModel>> {
        return Mono.just(DatedResponse(SIMPLE_MODEL, LAST_MODIFIED_DATE))
    }

    override fun getSimpleModelWithNonStandardStatus(): Mono<HttpResponse<SimpleModel>> {
        return Mono.just(HttpResponse.created(SIMPLE_MODEL))
    }

    override fun getDatedSimpleModelWithNonMappedHeader(): Mono<HttpResponse<DatedResponse<SimpleModel>>> {
        val datedResponse = DatedResponse(SIMPLE_MODEL, LAST_MODIFIED_DATE)
        return Mono.just(HttpResponse.ok(datedResponse)
                .header("custom-header", "custom-value"))
    }

    override fun getSimpleModelWithNonMappedHeader(): Mono<HttpResponse<SimpleModel>> {
        return Mono.just(HttpResponse.ok(SIMPLE_MODEL)
                .header("custom-header", "custom-value-2"))
    }

    override fun getErrorResponse(): Mono<Void> {
        return Mono.fromCallable { throw HttpStatusException(HttpStatus.NOT_FOUND, "This is the error") }
    }

    override fun getFile(): Mono<FileCustomizableResponseType> {
        val stream = ByteArrayInputStream("My file content".toByteArray())
        return Mono.just(StreamedFile(stream, MediaType.TEXT_PLAIN_TYPE))
    }

    override fun getModelWithValidatedList(): Mono<ModelWithValidatedListProperty> {
        return Mono.just(ModelWithValidatedListProperty(objectList = listOf(SimpleModel(color = "a"))))
    }

    companion object {

        @JvmField
        val SIMPLE_MODEL = SimpleModel("red", 10L, 10.5F, null, false, listOf("1,1", "2,2", "2,4"))

        val SIMPLE_MODELS = listOf(
                SIMPLE_MODEL,
                SimpleModel("red", 3L, 10.5F),
                SimpleModel(color = "blue", state = StateEnum.RUNNING, points = listOf("1,1", "2,2", "3,3")),
        )

        @JvmField
        val DATE_TIME_INSTANCE = OffsetDateTime.parse("2022-12-04T11:35:00.784Z")
                .atZoneSameInstant(ZoneId.of("America/Toronto"))

        val DATE_MODEL_INSTANCE = DateModel(LocalDate.of(2023, 6, 27), DATE_TIME_INSTANCE)

        const val LAST_MODIFIED_STRING = "2023-01-24T10:15:59.100+06:00"
        val LAST_MODIFIED_DATE = ZonedDateTime.parse(LAST_MODIFIED_STRING)
    }
}
