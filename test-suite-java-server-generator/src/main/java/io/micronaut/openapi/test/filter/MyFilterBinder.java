package io.micronaut.openapi.test.filter;

import java.util.Optional;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;
import io.micronaut.http.exceptions.HttpStatusException;

import jakarta.inject.Singleton;

/**
 * A custom parameter binder for MyFilter parameter type.
 */
@Singleton
final class MyFilterBinder implements TypedRequestArgumentBinder<MyFilter> {

    public static final String HEADER_NAME = "Filter";

    @Override
    public Argument<MyFilter> argumentType() {
        return Argument.of(MyFilter.class);
    }

    @Override
    public BindingResult<MyFilter> bind(
        ArgumentConversionContext<MyFilter> context,
        HttpRequest<?> source
    ) {
        String filter = source.getHeaders().get(HEADER_NAME);
        return () -> {
            try {
                return Optional.of(MyFilter.parse(filter));
            } catch (MyFilter.ParseException e) {
                throw new HttpStatusException(HttpStatus.BAD_REQUEST,
                    "Could not parse the " + HEADER_NAME + " query parameter. " + e.getMessage());
            }
        };
    }

}
