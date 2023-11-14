package io.micronaut.openapi.test.api

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.openapi.test.model.*
import io.micronaut.openapi.test.model.ColorEnum.Companion.fromValue
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream

@Controller
open class RequestBodyController : RequestBodyApi {

    @Post(value = "/echo", consumes = ["text/plain"], produces = ["text/plain"])
    fun echo(request: String): String {
        return request
    }

    override fun sendSimpleModel(simpleModel: SimpleModel?): Mono<SimpleModel> {
        return Mono.just(simpleModel!!)
    }

    override fun sendValidatedCollection(requestBody: List<List<String>>?): Mono<Void> {
        return Mono.empty()
    }

    override fun sendListOfSimpleModels(simpleModels: List<SimpleModel>?): Mono<List<SimpleModel>> {
        return Mono.just(simpleModels!!)
    }

    override fun sendModelWithRequiredProperties(model: ModelWithRequiredProperties?): Mono<ModelWithRequiredProperties> {
        return Mono.just(model!!)
    }

    override fun sendDateModel(model: DateModel?): Mono<DateModel> {
        return Mono.just(model!!)
    }

    override fun sendEnum(color: String): Mono<ColorEnum> {
        return Mono.just(fromValue(color.replace("\"", "")))
    }

    override fun sendEnumList(availableColors: List<ColorEnum>): Mono<List<ColorEnum>> {
        return Mono.just(availableColors)
    }

    override fun sendModelWithMapProperty(model: ModelWithMapProperty): Mono<ModelWithMapProperty> {
        return Mono.just(model)
    }

    override fun sendModelWithValidatedListProperty(modelWithValidatedListProperty: ModelWithValidatedListProperty): Mono<Void> {
        return Mono.empty()
    }

    override fun sendNestedModel(model: NestedModel): Mono<NestedModel> {
        return Mono.just(model)
    }

    override fun sendModelWithInnerEnum(model: ModelWithInnerEnum): Mono<ModelWithInnerEnum> {
        return Mono.just(model)
    }

    override fun sendModelWithDiscriminator(model: Animal): Mono<Animal> {
        return Mono.just(model)
    }

    override fun sendBytes(bytes: ByteArray?): Mono<ByteArray> {
        return Mono.just(bytes!!)
    }

    override fun sendModelWithEnumList(model: ModelWithEnumList): Mono<ModelWithEnumList> {
        return Mono.just(model)
    }

    override fun sendFile(file: CompletedFileUpload?): Mono<ByteArray> {
        return Mono.fromCallable {
            val inputStream = file!!.inputStream
            val outputStream = ByteArrayOutputStream()
            outputStream.write("name: ".toByteArray())
            outputStream.write(file.filename.toByteArray())
            outputStream.write(", content: ".toByteArray())
            inputStream.transferTo(outputStream)
            inputStream.close()
            outputStream.toByteArray()
        }
    }
}
