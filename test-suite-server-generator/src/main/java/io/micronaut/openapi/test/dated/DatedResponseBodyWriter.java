package io.micronaut.openapi.test.dated;


import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.MutableHeaders;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.body.MessageBodyHandlerRegistry;
import io.micronaut.http.body.MessageBodyWriter;
import io.micronaut.http.codec.CodecException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.OutputStream;
import java.util.List;

/**
 * An class for writing {@link DatedResponse} to the HTTP response with JSON body.
 *
 * @param <T> the type of the response body
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Order(-1)
final class DatedResponseBodyWriter<T> implements MessageBodyWriter<DatedResponse<T>> {

    private static final String LAST_MODIFIED_HEADER = "Last-Modified";

    private final MessageBodyHandlerRegistry registry;
    private final MessageBodyWriter<T> bodyWriter;
    private final Argument<T> bodyType;

    @Inject
    DatedResponseBodyWriter(MessageBodyHandlerRegistry registry) {
        this(registry, null, null);
    }

    private DatedResponseBodyWriter(
        MessageBodyHandlerRegistry registry,
        @Nullable MessageBodyWriter<T> bodyWriter,
        @Nullable Argument<T> bodyType
    ) {
        this.registry = registry;
        this.bodyWriter = bodyWriter;
        this.bodyType = bodyType;
    }

    @Override
    public MessageBodyWriter<DatedResponse<T>> createSpecific(
        Argument<DatedResponse<T>> type
    ) {
        Argument<T> bt = type.getTypeParameters()[0];
        MessageBodyWriter<T> writer = registry.findWriter(bt, List.of(MediaType.APPLICATION_JSON_TYPE))
                .orElseThrow(() -> new ConfigurationException("No JSON message writer present"));
        return new DatedResponseBodyWriter<>(registry, writer, bt);
    }

    @Override
    public void writeTo(
        Argument<DatedResponse<T>> type,
        MediaType mediaType,
        DatedResponse<T> dated,
        MutableHeaders headers,
        OutputStream outputStream
    ) throws CodecException {
        if (bodyType != null && bodyWriter != null) {
            headers.add(LAST_MODIFIED_HEADER, dated.getLastModified().toString());
            bodyWriter.writeTo(bodyType, mediaType, dated.getBody(), headers, outputStream);
        } else {
            throw new ConfigurationException("No JSON message writer present");
        }
    }

    @Override
    public boolean isWriteable(
        Argument<DatedResponse<T>> type,
        MediaType mediaType
    ) {
        return bodyType != null && bodyWriter != null && bodyWriter.isWriteable(bodyType, mediaType);
    }

    @Override
    public boolean isBlocking() {
        return bodyWriter != null && bodyWriter.isBlocking();
    }

}
