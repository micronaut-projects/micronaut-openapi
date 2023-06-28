package io.micronaut.openapi.test.api;

import io.micronaut.data.model.Pageable;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.openapi.test.model.SimpleModel;
import io.micronaut.openapi.test.model.StateEnum;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.exceptions.HttpStatusException;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.List;

@Controller
public class ResponseBodyController implements ResponseBodyApi {

    public static SimpleModel SIMPLE_MODEL =
            new SimpleModel()
                    .color("red")
                    .area(10.5f)
                    .numEdges(10L)
                    .convex(false)
                    .points(List.of("1,1", "2,2", "2,4"));

    public static List<SimpleModel> SIMPLE_MODELS =
            List.of(
                    SIMPLE_MODEL,
                    new SimpleModel().color("red").area(10.5f).numEdges(3L),
                    new SimpleModel()
                            .color("blue")
                            .state(StateEnum.RUNNING)
                            .points(List.of("1,1", "2,2", "3,3")));

    @Override
    public Mono<SimpleModel> getSimpleModel() {
        return Mono.just(SIMPLE_MODEL);
    }

    @Override
    public Mono<List<SimpleModel>> getPaginatedSimpleModel(Pageable pageable) {
        return Mono.just(SIMPLE_MODELS);
    }

    @Override
    public Mono<SimpleModel> getDatedSimpleModel() {
        return Mono.just(SIMPLE_MODEL);
    }

    @Override
    public Mono<SimpleModel> getSimpleModelWithNonStandardStatus() {
        return Mono.just(SIMPLE_MODEL);
    }

    @Override
    public Mono<SimpleModel> getDatedSimpleModelWithNonMappedHeader() {
        return Mono.just(SIMPLE_MODEL);
    }

    @Override
    public Mono<SimpleModel> getSimpleModelWithNonMappedHeader() {
        return Mono.just(SIMPLE_MODEL);
    }

    @Override
    public Mono<Void> getErrorResponse() {
        return Mono.fromCallable(() -> {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "This is the error");
        });
    }

    @Override
    public Mono<CompletedFileUpload> getFile() {
        ByteArrayInputStream stream = new ByteArrayInputStream("My file content".getBytes());
        return Mono.empty();
    }
}
