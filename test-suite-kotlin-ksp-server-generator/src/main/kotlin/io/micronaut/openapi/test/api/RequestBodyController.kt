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

    override fun sendListOfSimpleModels(simpleModel: List<SimpleModel>?): Mono<List<SimpleModel>> {
        return Mono.just(simpleModel!!)
    }

    override fun sendModelWithRequiredProperties(modelWithRequiredProperties: ModelWithRequiredProperties?): Mono<ModelWithRequiredProperties> {
        return Mono.just(modelWithRequiredProperties!!)
    }

    override fun sendDateModel(dateModel: DateModel?): Mono<DateModel> {
        return Mono.just(dateModel!!)
    }

    override fun sendEnum(body: String): Mono<ColorEnum> {
        return Mono.just(fromValue(body.replace("\"", "")))
    }

    override fun sendEnumList(colorEnums: List<ColorEnum>): Mono<List<ColorEnum>> {
        return Mono.just(colorEnums)
    }

    override fun sendModelWithMapProperty(modelWithMapProperty: ModelWithMapProperty): Mono<ModelWithMapProperty> {
        return Mono.just(modelWithMapProperty)
    }

    override fun sendModelWithValidatedListProperty(modelWithValidatedListProperty: ModelWithValidatedListProperty): Mono<Void> {
        return Mono.empty()
    }

    override fun sendNestedModel(nestedModel: NestedModel): Mono<NestedModel> {
        return Mono.just(nestedModel)
    }

    override fun sendModelWithInnerEnum(modelWithInnerEnum: ModelWithInnerEnum): Mono<ModelWithInnerEnum> {
        return Mono.just(modelWithInnerEnum)
    }

    override fun sendModelWithDiscriminator(animal: Animal): Mono<Animal> {
        return Mono.just(animal)
    }

    override fun sendBytes(body: ByteArray?): Mono<ByteArray> {
        return Mono.just(body!!)
    }

    override fun sendModelWithEnumList(modelWithEnumList: ModelWithEnumList): Mono<ModelWithEnumList> {
        return Mono.just(modelWithEnumList)
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
