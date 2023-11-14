package io.micronaut.openapi.test.dated

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import java.time.ZonedDateTime

/**
 * A response that contains information about last modification.
 *
 * @param <T> The response body type.
</T> */
data class DatedResponse<T>(
    @NonNull
    var body: T,
    @Nullable
    var lastModified: ZonedDateTime?,
)
