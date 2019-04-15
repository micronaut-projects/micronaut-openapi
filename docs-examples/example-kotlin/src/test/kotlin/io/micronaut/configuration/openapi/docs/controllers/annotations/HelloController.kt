package io.micronaut.configuration.openapi.docs.controllers.annotations

// tag::imports[]
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.validation.Validated
import io.reactivex.Single
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

// end::imports[]
// tag::clazz[]
@Controller("/")
@Validated
open class HelloController {

    /**
     * @param name The person's name
     * @return The greeting message
     */
    @Get(uri = "/greetings/{name}", produces = [MediaType.TEXT_PLAIN])
    // Please Note: Repeatable Annotations with non-SOURCE retentions are not yet supported with Kotlin so we are using `responses`
    // in `@Operations`, instead of `@ApiResponse`, see https://youtrack.jetbrains.com/issue/KT-12794
    @Operation(summary = "Greets a person", description = "A friendly greeting is returned",
            responses = [
                ApiResponse(content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
                ApiResponse(responseCode = "400", description = "Invalid Name Supplied"),
                ApiResponse(responseCode = "404", description = "Person not found")
            ])
    @Tag(name = "greeting")
    open fun greetings(name: String): Single<String> {
        return Single.just("Hello $name, how are you doing?")
    }
}
// end::clazz[]