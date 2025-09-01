package io.micronaut.openapi.test.filter

import io.micronaut.core.bind.ArgumentBinder
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import java.util.*

/**
 * A custom parameter binder for MyFilter parameter type.
 */
@Singleton
internal class MyFilterBinder : TypedRequestArgumentBinder<MyFilter> {

    override fun argumentType(): Argument<MyFilter> {
        return Argument.of(MyFilter::class.java)
    }

    override fun bind(
            context: ArgumentConversionContext<MyFilter>,
            source: HttpRequest<*>
    ): ArgumentBinder.BindingResult<MyFilter> {
        val filter = source.headers[HEADER_NAME]
        return ArgumentBinder.BindingResult {
            try {
                return@BindingResult Optional.of<MyFilter>(MyFilter.parse(filter))
            } catch (e: MyFilter.ParseException) {
                throw HttpStatusException(HttpStatus.BAD_REQUEST, "Could not parse the $HEADER_NAME query parameter. ${e.message}")
            }
        }
    }

    companion object {
        const val HEADER_NAME = "Filter"
    }
}
