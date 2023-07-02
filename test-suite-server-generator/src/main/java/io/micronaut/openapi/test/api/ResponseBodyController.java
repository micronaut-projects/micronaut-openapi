package io.micronaut.openapi.test.api;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.server.types.files.FileCustomizableResponseType;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.openapi.test.model.DateModel;
import io.micronaut.openapi.test.dated.DatedResponse;
import io.micronaut.openapi.test.model.SimpleModel;
import io.micronaut.openapi.test.model.StateEnum;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.exceptions.HttpStatusException;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    public static ZonedDateTime DATE_TIME_INSTANCE =
        OffsetDateTime.parse("2022-12-04T11:35:00.784Z")
            .atZoneSameInstant(ZoneId.of("America/Toronto"));

    public static DateModel DATE_MODEL_INSTANCE = new DateModel()
        .commitDate(LocalDate.of(2023, 6, 27))
        .commitDateTime(DATE_TIME_INSTANCE);

    public static final String LAST_MODIFIED_STRING = "2023-01-24T10:15:59.100+06:00";

    public static final ZonedDateTime LAST_MODIFIED_DATE =
        ZonedDateTime.parse(LAST_MODIFIED_STRING);

    @Override
    public Mono<SimpleModel> getSimpleModel() {
        return Mono.just(SIMPLE_MODEL);
    }

    @Override
    public Mono<ZonedDateTime> getDateTime() {
        return Mono.just(DATE_TIME_INSTANCE);
    }

    @Override
    public Mono<DateModel> getDateModel() {
        return Mono.just(DATE_MODEL_INSTANCE);
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
    public HttpResponse<Mono<SimpleModel>> getSimpleModelWithNonStandardStatus() {
        return HttpResponse.created(Mono.just(SIMPLE_MODEL));
    }

    @Override
    public HttpResponse<Mono<DatedResponse<SimpleModel>>> getDatedSimpleModelWithNonMappedHeader() {
        DatedResponse<SimpleModel> datedResponse = DatedResponse.of(SIMPLE_MODEL)
            .withLastModified(LAST_MODIFIED_DATE);
        return HttpResponse.ok(Mono.just(datedResponse))
            .header("custom-header", "custom-value");
    }

    @Override
    public HttpResponse<Mono<SimpleModel>> getSimpleModelWithNonMappedHeader() {
        return HttpResponse.ok(Mono.just(SIMPLE_MODEL))
            .header("custom-header", "custom-value-2");
    }

    @Override
    public Mono<Void> getErrorResponse() {
        return Mono.fromCallable(() -> {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "This is the error");
        });
    }

    @Override
    public Mono<FileCustomizableResponseType> getFile() {
        ByteArrayInputStream stream = new ByteArrayInputStream("My file content".getBytes());
        return Mono.just(new StreamedFile(stream, MediaType.TEXT_PLAIN_TYPE));
    }
}
