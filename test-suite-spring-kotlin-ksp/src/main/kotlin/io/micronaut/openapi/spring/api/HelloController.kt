package io.micronaut.openapi.spring.api

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api")
class HelloController {

    @GetMapping
    fun endpoint(): ResponseEntity<ResponseObject<List<Dto>>> {
        return ResponseEntity.ok(ResponseObject())
    }

    @PostMapping("/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun endpoint2(@RequestParam files: MultiValueMap<String?, MultipartFile?>?) {
        println("endpoint2")
    }

    @PostMapping("/file2", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun endpoint3(files: MultipartFile?) {
        println("endpoint3")
    }

    class ResponseObject<T>(
        var body: T? = null,
    )

    class Dto(
        var locale: Locale? = null,
    )
}