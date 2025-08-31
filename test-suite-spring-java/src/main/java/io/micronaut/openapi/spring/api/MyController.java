package io.micronaut.openapi.spring.api;

// tag::imports[]
import io.micronaut.openapi.annotation.OpenAPIRequest;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY;

// end::imports[]
// tag::clazz[]
@RestController
@RequestMapping("/my-api")
public class MyController {

    @PostMapping(value = "/testMultipart/{pathVar}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void testMultipart(@OpenAPIRequest WrappedRq rq) {
        System.out.println(rq);
    }

    /**
     * Wrapped request data
     */
    public static class WrappedRq {

        /**
         * This is path variable
         */
        private String pathVar;
        /**
         * This is request param
         */
        @Parameter(in = QUERY) // you need to set, that this is @RequestParam by swagger annotation!
        private String queryVar;
        /**
         * This is fileName request body part
         */
        // Part from multipart body
        private String fileName;
        /**
         * This is file request body part
         */
        // Part from multipart body
        private MultipartFile file;

        public String getPathVar() {
            return pathVar;
        }

        public void setPathVar(String pathVar) {
            this.pathVar = pathVar;
        }

        public String getQueryVar() {
            return queryVar;
        }

        public void setQueryVar(String queryVar) {
            this.queryVar = queryVar;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public MultipartFile getFile() {
            return file;
        }

        public void setFile(MultipartFile file) {
            this.file = file;
        }
    }
}
//end::clazz[]
