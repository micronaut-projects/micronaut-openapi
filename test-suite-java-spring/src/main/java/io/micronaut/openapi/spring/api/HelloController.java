package io.micronaut.openapi.spring.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping
    public ResponseEntity<ResponseObject<List<Dto>>> endpoint() {
        return ResponseEntity.ok(new ResponseObject<>());
    }

    public static class ResponseObject<T> {

        public T body;
    }

    public static class Dto {

        public Locale locale;
    }
}