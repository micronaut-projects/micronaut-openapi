package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI

class OpenApiPageSpec extends AbstractOpenApiTypeElementSpec {

    void "test openAPI micronaut data page"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller("/")
class MyController {

    @Post
    public Page<MyDto> getSomeDTOs(@Body Pageable pageable) {
        return null;
    }
}

class MyDto {

    private String parameters;

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.components.schemas
    }
}
