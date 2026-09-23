package io.micronaut.configuration.openapi.docs.versions

// tag::imports[]
import io.micronaut.core.version.annotation.Version
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
// end::imports[]

// tag::clazz[]
@Controller("/versioned")
class VersionedController {

    @Version("1")
    @Get("/hello")
    fun helloV1(): String = "helloV1"

    @Version("2")
    @Post("/hello")
    fun helloV2(userDto: UserDto): String = "helloV2"

    @Post("/common")
    fun common(): String? = null
}
// end::clazz[]
