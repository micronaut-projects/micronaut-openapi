/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.parameters.RequestBody

class OpenApiRequestBodyArraySchemaSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    void "test parse the OpenAPI with ArraySchema in RequestBody"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.List;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller("/")
class MyController {

    @Put("/")
    @Tag(name = "Tag name", description = "tag description.")
    @Operation(description = "Operation description.", summary = "Operation summary.")
    @RequestBody(description = "Body description.",
            required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    array = @ArraySchema(schema = @Schema(implementation = Long.class)
                    )))
    public void update(List<Long> l) {
    }
}

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI
        openAPI.paths.size() == 1
        openAPI.paths.get("/")
        openAPI.paths.get("/").put

        when:
        Operation operation = openAPI.paths?.get("/")?.put
        RequestBody requestBody = operation.requestBody

        then:
        requestBody.required
        requestBody.description == "Body description."
        requestBody.content
        requestBody.content.size() == 1
        requestBody.content['application/json'].schema
        requestBody.content['application/json'].schema instanceof ArraySchema

        expect:
        operation
        operation.responses.size() == 1
    }

}