package io.micronaut.openapi.spring.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping
    public ResponseEntity<ResponseObject<List<Dto>>> endpoint() {
        return ResponseEntity.ok(new ResponseObject<>());
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void endpoint2(@RequestParam MultiValueMap<String, MultipartFile> files) {
        System.out.println("endpoint2");
    }

    @PostMapping(value = "/file2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void endpoint3(MultipartFile files) {
        System.out.println("endpoint3");
    }

    public static class ResponseObject<T> {

        public T body;
    }

    public static class Dto {

        public Locale locale;
    }
}
