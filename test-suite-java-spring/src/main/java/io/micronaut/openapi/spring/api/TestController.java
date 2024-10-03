package io.micronaut.openapi.spring.api;

import io.micronaut.openapi.spring.api.dto.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

@RestController("/user")
class TestController {

    /**
     * {@summary Create post op summary.} Operation post description.
     *
     * @param user User request body
     *
     * @return created post user
     */
    @PostMapping(value = "/create",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public User createPost(@RequestBody User user) {
        user.setId(9876L);
        return user;
    }

    /**
     * {@summary Create patch op summary.} Operation patch description.
     *
     * @param user User request body
     */
    @PatchMapping("/create")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public void createPatch(@RequestBody(required = false) User user) {
    }

    @GetMapping(value = "/{userId}", produces = MediaType.TEXT_HTML_VALUE)
    public String get(
        @PathVariable String userId,
        @RequestParam(required = false, defaultValue = "123") Integer age
    ) {
        return "Pong userId " + userId;
    }

    @PatchMapping(value = "/patch")
    public User patch(
        @RequestBody User user,
        @SessionAttribute(name = "mySesAttr", required = false) String sesAttr
    ) {
        user.setId(9876L);
        return user;
    }

    @GetMapping("/pageable")
    public Page<User> getSomeDTOs(Pageable pageable) {
        return null;
    }

}
