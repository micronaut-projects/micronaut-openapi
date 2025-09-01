package io.micronaut.openapi.spring.api

// tag::imports[]
import io.micronaut.openapi.annotation.OpenAPIRequest
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

// end::imports[]
// tag::clazz[]
@RestController
@RequestMapping("/my-api")
class MyController {

    @PostMapping("/testMultipart/{pathVar}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun testMultipart(@OpenAPIRequest rq: WrappedRq?) {
        println(rq)
    }

    /**
     * Wrapped request data
     */
    class WrappedRq(
        /**
         * This is path variable
         */
        var pathVar: String? = null,

        /**
         * This is request param
         */
        @field:Parameter(`in` = ParameterIn.QUERY) // you need to set, that this is @RequestParam by swagger annotation!
        var queryVar: String? = null,

        /**
         * This is fileName request body part
         */
        // Part from multipart body
        var fileName: String? = null,

        /**
         * This is file request body part
         */
        // Part from multipart body
        var file: MultipartFile? = null,
    )
}
//end::clazz[]
