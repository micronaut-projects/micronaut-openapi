package io.micronaut.openapi.test.api;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.types.files.FileCustomizableResponseType;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.openapi.test.dated.DatedResponse;
import io.micronaut.openapi.test.model.DateModel;
import io.micronaut.openapi.test.model.SimpleModel;
import io.micronaut.openapi.test.model.StateEnum;

import reactor.core.publisher.Mono;

@Controller
public class ResponseBodyController implements ResponseBodyApi {

    public static SimpleModel SIMPLE_MODEL;
    static {
        var simpleModel = new SimpleModel();
        simpleModel.setColor("red");
        simpleModel.setArea(10.5f);
        simpleModel.setNumEdges(10L);
        simpleModel.setConvex(false);
        simpleModel.setPoints(List.of("1,1", "2,2", "2,4"));
        SIMPLE_MODEL = simpleModel;
    }

    public static List<SimpleModel> SIMPLE_MODELS;
    static {
        var simpleModel1 = new SimpleModel();
        simpleModel1.setColor("red");
        simpleModel1.setArea(10.5f);
        simpleModel1.setNumEdges(3L);

        var simpleModel2 = new SimpleModel();
        simpleModel2.setColor("blue");
        simpleModel2.setState(StateEnum.RUNNING);
        simpleModel2.setPoints(List.of("1,1", "2,2", "3,3"));

        SIMPLE_MODELS = List.of(
            SIMPLE_MODEL,
            simpleModel1,
            simpleModel2
        );
    }

    public static ZonedDateTime DATE_TIME_INSTANCE = OffsetDateTime.parse("2022-12-04T11:35:00.784Z")
        .atZoneSameInstant(ZoneId.of("America/Toronto"));

    public static DateModel DATE_MODEL_INSTANCE;
    static {
        var dateModel = new DateModel();
        dateModel.setCommitDate(LocalDate.of(2023, 6, 27));
        dateModel.setCommitDateTime(DATE_TIME_INSTANCE);
        DATE_MODEL_INSTANCE = dateModel;
    }

    public static final String LAST_MODIFIED_STRING = "2023-01-24T10:15:59.100+06:00";
    public static final ZonedDateTime LAST_MODIFIED_DATE = ZonedDateTime.parse(LAST_MODIFIED_STRING);

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
    public Mono<HttpResponse<SimpleModel>> getSimpleModelWithNonStandardStatus() {
        return Mono.just(HttpResponse.created(SIMPLE_MODEL));
    }

    @Override
    public Mono<HttpResponse<DatedResponse<SimpleModel>>> getDatedSimpleModelWithNonMappedHeader() {
        DatedResponse<SimpleModel> datedResponse = DatedResponse.of(SIMPLE_MODEL)
            .withLastModified(LAST_MODIFIED_DATE);
        return Mono.just(HttpResponse.ok(datedResponse)
            .header("custom-header", "custom-value"));
    }

    @Override
    public Mono<HttpResponse<SimpleModel>> getSimpleModelWithNonMappedHeader() {
        return Mono.just(HttpResponse.ok(SIMPLE_MODEL)
            .header("custom-header", "custom-value-2"));
    }

    @Override
    public Mono<Void> getErrorResponse() {
        return Mono.fromCallable(() -> {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "This is the error");
        });
    }

    @Override
    public Mono<FileCustomizableResponseType> getFile() {
        var stream = new ByteArrayInputStream("My file content".getBytes());
        return Mono.just(new StreamedFile(stream, MediaType.TEXT_PLAIN_TYPE));
    }
}
