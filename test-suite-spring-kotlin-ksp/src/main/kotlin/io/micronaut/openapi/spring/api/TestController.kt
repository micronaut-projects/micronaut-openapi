package io.micronaut.openapi.spring.api

import io.micronaut.openapi.spring.api.dto.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController("/user")
internal class TestController {
    /**
     * {@summary Create post op summary.} Operation post description.
     *
     * @param user User request body
     *
     * @return created post user
     */
    @PostMapping("/create", produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createPost(@RequestBody user: User): User {
        user.id = 9876L
        return user
    }

    /**
     * {@summary Create patch op summary.} Operation patch description.
     *
     * @param user User request body
     */
    @PatchMapping("/create")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    fun createPatch(@RequestBody(required = false) user: User?) {
    }

    @GetMapping("/{userId}", produces = [MediaType.TEXT_HTML_VALUE])
    fun get(
        @PathVariable userId: String,
        @RequestParam(required = false, defaultValue = "123") age: Int?
    ): String {
        return "Pong userId $userId"
    }

    @PatchMapping("/patch")
    fun patch(
        @RequestBody user: User,
        @SessionAttribute(name = "mySesAttr", required = false) sesAttr: String?
    ): User {
        user.id = 9876L
        return user
    }

    @GetMapping("/pageable")
    fun getSomeDTOs(pageable: Pageable?): Page<User>? {
        return null
    }
}