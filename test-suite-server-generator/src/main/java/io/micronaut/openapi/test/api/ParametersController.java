package io.micronaut.openapi.test.api;

import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.openapi.test.filter.MyFilter;
import io.micronaut.openapi.test.model.SendDatesResponse;
import io.micronaut.openapi.test.model.SendPrimitivesResponse;
import io.micronaut.http.annotation.Controller;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

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
            LocalDate commitDate, ZonedDateTime commitDateTime) {
        return Mono.just(new SendDatesResponse()
                .commitDate(commitDate)
                .commitDateTime(commitDateTime));
    }

    @Override
    public Mono<String> getIgnoredHeader() {
        return Mono.just("Success");
    }

    @Override
    public Mono<String> sendIgnoredHeader() {
        return Mono.just("Success");
    }

    @Override
    public Mono<String> sendPageQuery(Pageable pageable) {
        return Mono.just(
            "(page: " + pageable.getNumber() +
            ", size: " + pageable.getSize() +
            ", sort: " + sortToString(pageable.getSort()) + ")"
        );
    }

    @Override
    public Mono<String> sendMappedParameter(MyFilter myFilter) {
        return Mono.just(myFilter.toString());
    }

    private String sortToString(Sort sort) {
        return sort.getOrderBy().stream().map(
            order -> order.getProperty() + "(dir=" + order.getDirection() + ")"
        ).collect(Collectors.joining(" "));
    }
}
