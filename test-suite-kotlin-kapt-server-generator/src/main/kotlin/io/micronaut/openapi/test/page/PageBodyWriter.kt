package io.micronaut.openapi.test.page

import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.annotation.Order
import io.micronaut.core.type.Argument
import io.micronaut.core.type.MutableHeaders
import io.micronaut.data.model.Page
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Produces
import io.micronaut.http.body.MessageBodyHandlerRegistry
import io.micronaut.http.body.MessageBodyWriter
import io.micronaut.http.codec.CodecException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.OutputStream

/**
 * A class for writing [Page] to the HTTP response with content as JSON body.
 *
 * @param T the type of page item
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Order(-1)
internal class PageBodyWriter<T>(
    val registry: MessageBodyHandlerRegistry,
    val bodyWriter: @Nullable MessageBodyWriter<List<T>>?,
    val bodyType: @Nullable Argument<List<T>>?
) : MessageBodyWriter<Page<T>> {

    @Inject
    constructor(registry: MessageBodyHandlerRegistry) : this(registry, null, null)

    override fun createSpecific(type: Argument<Page<T>>): MessageBodyWriter<Page<T>> {
        val bt = Argument.listOf(type.typeParameters[0]) as Argument<List<T>>
        val writer = registry.findWriter(bt, listOf(MediaType.APPLICATION_JSON_TYPE))
                .orElseThrow { ConfigurationException("No JSON message writer present") }
        return PageBodyWriter(registry, writer, bt)
    }

    @Throws(CodecException::class)
    override fun writeTo(
            type: Argument<Page<T>>,
            mediaType: MediaType,
            page: Page<T>,
            headers: MutableHeaders,
            outputStream: OutputStream
    ) {
        if (bodyType == null || bodyWriter == null) {
            throw ConfigurationException("No JSON message writer present")
        }
        headers.add(PAGE_NUMBER_HEADER, page.pageNumber.toString())
                .add(PAGE_SIZE_HEADER, page.size.toString())
                .add(PAGE_COUNT_HEADER, page.totalPages.toString())
                .add(TOTAL_COUNT_HEADER, page.totalSize.toString())
        bodyWriter.writeTo(bodyType, mediaType, page.content, headers, outputStream)
    }

    override fun isWriteable(type: Argument<Page<T>>, mediaType: MediaType): Boolean {
        return bodyType == null || bodyWriter != null && bodyWriter.isWriteable(bodyType, mediaType)
    }

    override fun isBlocking(): Boolean {
        return bodyWriter != null && bodyWriter.isBlocking
    }

    companion object {
        private const val PAGE_NUMBER_HEADER = "X-Page-Number"
        private const val PAGE_SIZE_HEADER = "X-Page-Size"
        private const val TOTAL_COUNT_HEADER = "X-Total-Count"
        private const val PAGE_COUNT_HEADER = "X-Page-Count"
    }
}
