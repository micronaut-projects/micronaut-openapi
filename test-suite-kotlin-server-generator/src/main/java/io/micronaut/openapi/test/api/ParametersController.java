package io.micronaut.openapi.test.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.http.annotation.Controller;
import io.micronaut.openapi.test.filter.MyFilter;
import io.micronaut.openapi.test.model.ColorEnum;
import io.micronaut.openapi.test.model.SendDatesResponse;
import io.micronaut.openapi.test.model.SendPrimitivesResponse;

import org.jetbrains.annotations.NotNull;

import reactor.core.publisher.Mono;

@Controller
public class ParametersController implements ParametersApi {

    @Override
    public Mono<SendPrimitivesResponse> sendPrimitives(String name, BigDecimal age, float height, boolean isPositive) {
        var response = new SendPrimitivesResponse();
        response.setName(name);
        response.setAge(age);
        response.setHeight(height);
        response.setPositive(isPositive);
        return Mono.just(response);
    }

    @NotNull
    @Override
    public Mono<String> sendValidatedPrimitives(String name, Integer age, BigDecimal favoriteNumber, Double height) {
        return Mono.just("Success");
    }

    @Override
    public Mono<SendDatesResponse> sendDates(LocalDate commitDate, ZonedDateTime commitDateTime) {
        var response = new SendDatesResponse();
        response.setCommitDate(commitDate);
        response.setCommitDateTime(commitDateTime);
        return Mono.just(response);
    }

    @Override
    public Mono<ColorEnum> sendParameterEnum(ColorEnum colorParam) {
        return Mono.just(colorParam);
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
        return sort.getOrderBy().stream()
            .map(
                order -> order.getProperty() + "(dir=" + order.getDirection() + ")"
            ).collect(Collectors.joining(" "));
    }
}
