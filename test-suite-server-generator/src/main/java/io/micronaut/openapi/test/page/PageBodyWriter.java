package io.micronaut.openapi.test.page;


import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.MutableHeaders;
import io.micronaut.data.model.Page;
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
 * An class for writing {@link Page} to the HTTP response with content as JSON body.
 *
 * @param <T> the type of page item
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Order(-1)
final class PageBodyWriter<T> implements MessageBodyWriter<Page<T>> {

    private static final String PAGE_NUMBER_HEADER = "X-Page-Number";
    private static final String PAGE_SIZE_HEADER = "X-Page-Size";
    private static final String TOTAL_COUNT_HEADER = "X-Total-Count";
    private static final String PAGE_COUNT_HEADER = "X-Page-Count";

    private final MessageBodyHandlerRegistry registry;
    private final MessageBodyWriter<List<T>> bodyWriter;
    private final Argument<List<T>> bodyType;

    @Inject
    PageBodyWriter(MessageBodyHandlerRegistry registry) {
        this(registry, null, null);
    }

    private PageBodyWriter(
        MessageBodyHandlerRegistry registry,
        @Nullable MessageBodyWriter<List<T>> bodyWriter,
        @Nullable Argument<List<T>> bodyType
    ) {
        this.registry = registry;
        this.bodyWriter = bodyWriter;
        this.bodyType = bodyType;
    }

    @Override
    public MessageBodyWriter<Page<T>> createSpecific(
        Argument<Page<T>> type
    ) {
        Argument<List<T>> bt = Argument.listOf(type.getTypeParameters()[0]);
        MessageBodyWriter<List<T>> writer = registry.findWriter(bt, List.of(MediaType.APPLICATION_JSON_TYPE))
                .orElseThrow(() -> new ConfigurationException("No JSON message writer present"));
        return new PageBodyWriter<>(registry, writer, bt);
    }

    @Override
    public void writeTo(
        Argument<Page<T>> type,
        MediaType mediaType,
        Page<T> page,
        MutableHeaders headers,
        OutputStream outputStream
    ) throws CodecException {
        if (bodyType != null && bodyWriter != null) {
            headers.add(PAGE_NUMBER_HEADER, String.valueOf(page.getPageNumber()));
            headers.add(PAGE_SIZE_HEADER, String.valueOf(page.getSize()));
            headers.add(PAGE_COUNT_HEADER, String.valueOf(page.getTotalPages()));
            headers.add(TOTAL_COUNT_HEADER, String.valueOf(page.getTotalSize()));

            bodyWriter.writeTo(bodyType, mediaType, page.getContent(), headers, outputStream);
        } else {
            throw new ConfigurationException("No JSON message writer present");
        }
    }

    @Override
    public boolean isWriteable(
        Argument<Page<T>> type,
        MediaType mediaType
    ) {
        return bodyType != null && bodyWriter != null && bodyWriter.isWriteable(bodyType, mediaType);
    }

    @Override
    public boolean isBlocking() {
        return bodyWriter != null && bodyWriter.isBlocking();
    }

}
