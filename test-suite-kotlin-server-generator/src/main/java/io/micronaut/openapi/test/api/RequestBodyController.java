package io.micronaut.openapi.test.api;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.openapi.test.model.Animal;
import io.micronaut.openapi.test.model.ColorEnum;
import io.micronaut.openapi.test.model.DateModel;
import io.micronaut.openapi.test.model.ModelWithEnumList;
import io.micronaut.openapi.test.model.ModelWithInnerEnum;
import io.micronaut.openapi.test.model.ModelWithMapProperty;
import io.micronaut.openapi.test.model.ModelWithRequiredProperties;
import io.micronaut.openapi.test.model.ModelWithValidatedListProperty;
import io.micronaut.openapi.test.model.NestedModel;
import io.micronaut.openapi.test.model.SimpleModel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

@Controller
public class RequestBodyController implements RequestBodyApi {

    @Post(value = "/echo", consumes = "text/plain", produces = "text/plain")
    public String echo(String request) {
        return request;
    }

    @Override
    public Mono<SimpleModel> sendSimpleModel(SimpleModel simpleModel) {
        return Mono.just(simpleModel);
    }

    @Override
    public Mono<Void> sendValidatedCollection(List<? extends List<String>> requestBody) {
        return Mono.empty();
    }

    @Override
    public Mono<List<SimpleModel>> sendListOfSimpleModels(List<SimpleModel> simpleModels) {
        return Mono.just(simpleModels);
    }

    @Override
    public Mono<ModelWithRequiredProperties> sendModelWithRequiredProperties(ModelWithRequiredProperties model) {
        return Mono.just(model);
    }

    @Override
    public Mono<DateModel> sendDateModel(DateModel model) {
        return Mono.just(model);
    }

    @Override
    public Mono<ColorEnum> sendEnum(String color) {
        return Mono.just(ColorEnum.fromValue(color.replace("\"", "")));
    }

    @Override
    public Mono<List<ColorEnum>> sendEnumList(List<@Valid ? extends ColorEnum> availableColors) {
        return Mono.just((List<ColorEnum>) availableColors);
    }

    @Override
    public Mono<ModelWithMapProperty> sendModelWithMapProperty(@Body @NotNull @Valid ModelWithMapProperty model) {
        return Mono.just(model);
    }

    @Override
    public Mono<Void> sendModelWithValidatedListProperty(@Body @NotNull @Valid ModelWithValidatedListProperty model) {
        return Mono.empty();
    }

    @Override
    public Mono<NestedModel> sendNestedModel(NestedModel model) {
        return Mono.just(model);
    }

    @Override
    public Mono<ModelWithInnerEnum> sendModelWithInnerEnum(ModelWithInnerEnum model) {
        return Mono.just(model);
    }

    @Override
    public Mono<Animal> sendModelWithDiscriminator(Animal model) {
        return Mono.just(model);
    }

    @Override
    public Mono<byte[]> sendBytes(byte[] bytes) {
        return Mono.just(bytes);
    }

    @Override
    public Mono<ModelWithEnumList> sendModelWithEnumList(ModelWithEnumList model) {
        return Mono.just(model);
    }

    @Override
    public Mono<byte[]> sendFile(CompletedFileUpload file) {
        return Mono.fromCallable(() -> {
            InputStream inputStream = file.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("name: ".getBytes());
            outputStream.write(file.getFilename().getBytes());
            outputStream.write(", content: ".getBytes());
            inputStream.transferTo(outputStream);
            inputStream.close();
            return outputStream.toByteArray();
        });
    }
}
