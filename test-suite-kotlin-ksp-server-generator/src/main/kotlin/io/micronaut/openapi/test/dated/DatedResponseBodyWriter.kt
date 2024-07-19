package io.micronaut.openapi.test.dated

import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.annotation.Order
import io.micronaut.core.type.Argument
import io.micronaut.core.type.MutableHeaders
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Produces
import io.micronaut.http.body.MessageBodyHandlerRegistry
import io.micronaut.http.body.MessageBodyWriter
import io.micronaut.http.codec.CodecException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.OutputStream

/**
 * A class for writing [DatedResponse] to the HTTP response with JSON body.
 *
 * @param <T> the type of the response body
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Order(-1)
internal class DatedResponseBodyWriter<T> private constructor(
    val registry: MessageBodyHandlerRegistry,
    val bodyWriter: @Nullable MessageBodyWriter<T>?,
    val bodyType: @Nullable Argument<T>?
) : MessageBodyWriter<DatedResponse<T>> {

    @Inject
    constructor(registry: MessageBodyHandlerRegistry) : this(registry, null, null)

    override fun createSpecific(
            type: Argument<DatedResponse<T>>
    ): MessageBodyWriter<DatedResponse<T>> {
        val bt = type.typeParameters[0] as Argument<T>
        val writer = registry.findWriter(bt, listOf(MediaType.APPLICATION_JSON_TYPE))
                .orElseThrow { ConfigurationException("No JSON message writer present") }
        return DatedResponseBodyWriter(registry, writer, bt)
    }

    @Throws(CodecException::class)
    override fun writeTo(
            type: Argument<DatedResponse<T>>,
            mediaType: MediaType,
            dated: DatedResponse<T>,
            headers: MutableHeaders,
            outputStream: OutputStream
    ) {
        if (bodyType == null || bodyWriter == null) {
            throw ConfigurationException("No JSON message writer present")
        }
        if (dated.lastModified != null) {
            headers.add(LAST_MODIFIED_HEADER, dated.lastModified.toString())
        }
        bodyWriter.writeTo(bodyType, mediaType, dated.body, headers, outputStream)
    }

    override fun isWriteable(type: Argument<DatedResponse<T>>, mediaType: MediaType): Boolean {
        return bodyType == null || bodyWriter != null && bodyWriter.isWriteable(bodyType, mediaType)
    }

    override fun isBlocking(): Boolean {
        return bodyWriter != null && bodyWriter.isBlocking
    }

    companion object {
        private const val LAST_MODIFIED_HEADER = "Last-Modified"
    }
}
