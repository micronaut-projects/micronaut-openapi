package io.micronaut.openapi.test.api;

import io.micronaut.openapi.test.model.SendDatesResponse;
import io.micronaut.openapi.test.model.SendPrimitivesResponse;
import io.micronaut.http.annotation.Controller;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Controller
public class ParametersController implements ParametersApi {

    @Override
    public Mono<SendPrimitivesResponse> sendPrimitives(
            String name, BigDecimal age, Float height, Boolean isPositive) {
        return Mono.just(new SendPrimitivesResponse()
                .name(name)
                .age(age)
                .height(height)
                .isPositive(isPositive));
    }

    @Override
    public Mono<String> sendValidatedPrimitives(
            String name,
            Integer age,
            BigDecimal favoriteNumber,
            Double height) {
        return Mono.just("Success");
    }

    @Override
    public Mono<SendDatesResponse> sendDates(
            LocalDate commitDate, OffsetDateTime commitDateTime) {
        return Mono.just(new SendDatesResponse()
                .commitDate(commitDate)
                .commitDateTime(commitDateTime));
    }

    @Override
    public Mono<String> getIgnoredHeader() {
        return Mono.just("Success");
    }

    @Override
    public Mono<String> sendIgnoredHeader(String header) {
        return Mono.just("Success");
    }

    @Override
    public Mono<String> sendPageQuery(Integer page, Integer size, String sort) {
        return Mono.just("(page: " + page + ", size: " + size + ", sort: " + sort + ")");
    }
}
