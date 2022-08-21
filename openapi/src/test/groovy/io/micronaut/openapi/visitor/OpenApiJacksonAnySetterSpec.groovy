package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema

class OpenApiJacksonAnySetterSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI with JacksonAnySetter"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.Map;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

import com.fasterxml.jackson.annotation.JsonAnySetter;

@Controller
class OpenApiController {
    @Post
    public void postRaw(@Body MyDto body) {
    }
}

class MyDto {

    private String prop1;
    private String prop2;
    @JsonAnySetter
    private Map<String, Object> values;

    public String getProp1() {
        return prop1;
    }

    public void setProp1(String prop1) {
        this.prop1 = prop1;
    }

    public String getProp2() {
        return prop2;
    }

    public void setProp2(String prop2) {
        this.prop2 = prop2;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['MyDto']
        Operation operation = openAPI.paths."/".post

        then: "the components are valid"

        schema
        operation
        schema.properties
        schema.properties.size() == 2
        schema.properties.prop1
        schema.properties.prop2
        !schema.properties.values
    }
}
