package io.micronaut.openapi.test.api;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.openapi.test.dated.DatedResponse;
import io.micronaut.openapi.test.model.SimpleModel;
import io.micronaut.openapi.test.model.StateEnum;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.exceptions.HttpStatusException;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
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

    public static final String LAST_MODIFIED_STRING = "2023-01-24T10:15:59.100+06:00";

    public static final ZonedDateTime LAST_MODIFIED_DATE =
        ZonedDateTime.parse(LAST_MODIFIED_STRING);

    @Override
    public Mono<SimpleModel> getSimpleModel() {
        return Mono.just(SIMPLE_MODEL);
    }

    @Override
    public Mono<Page<SimpleModel>> getPaginatedSimpleModel(Pageable pageable) {
        return Mono.just(Page.of(SIMPLE_MODELS, pageable, SIMPLE_MODELS.size()));
    }

    @Override
    public Mono<DatedResponse<SimpleModel>> getDatedSimpleModel() {
        return Mono.just(DatedResponse.of(SIMPLE_MODEL).withLastModified(LAST_MODIFIED_DATE));
    }

    @Override
    public Mono<SimpleModel> getSimpleModelWithNonStandardStatus() {
        return Mono.just(SIMPLE_MODEL);
    }

    @Override
    public Mono<DatedResponse<SimpleModel>> getDatedSimpleModelWithNonMappedHeader() {
        return Mono.just(DatedResponse.of(SIMPLE_MODEL));
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
