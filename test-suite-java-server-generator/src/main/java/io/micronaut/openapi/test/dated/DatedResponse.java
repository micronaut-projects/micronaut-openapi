package io.micronaut.openapi.test.dated;

import java.time.ZonedDateTime;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

/**
 * A response that contains information about last modification.
 *
 * @param <T> The response body type.
 */
public final class DatedResponse<T> {

    @Nullable
    private ZonedDateTime lastModified;

    @NonNull
    private final T body;

    private DatedResponse(T body, ZonedDateTime lastModified) {
        this.body = body;
        this.lastModified = lastModified;
    }

    /**
     * Set the last modified to this object.
     *
     * @param lastModified the last modification date.
     * @return this response.
     */
    public DatedResponse<T> withLastModified(ZonedDateTime lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    /**
     * @return The last modification date of returned resource.
     */
    public ZonedDateTime getLastModified() {
        return lastModified;
    }

    /**
     * @return The response body.
     */
    public T getBody() {
        return body;
    }

    /**
     * Create a response by specifying only the body.
     *
     * @param body The response body.
     * @return The response.
     * @param <T> The response body type.
     */
    public static <T> DatedResponse<T> of(@NonNull T body) {
        return new DatedResponse<>(body, null);
    }

    /**
     * Create a response by specifying both the body and last modification date of the resource.
     *
     * @param body The body.
     * @param lastModified The last modification date.
     * @return The response.
     * @param <T> The body type.
     */
    public static <T> DatedResponse<T> of(@NonNull T body, @Nullable ZonedDateTime lastModified) {
        return new DatedResponse<>(body, lastModified);
    }
}
