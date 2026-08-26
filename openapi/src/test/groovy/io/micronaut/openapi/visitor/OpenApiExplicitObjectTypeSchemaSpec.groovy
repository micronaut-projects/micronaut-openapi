package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.micronaut.openapi.swagger.core.util.PrimitiveType
import io.swagger.v3.oas.models.OpenAPI

class OpenApiExplicitObjectTypeSchemaSpec extends AbstractOpenApiTypeElementSpec {

    def setup() {
        PrimitiveType.explicitObjectType = true
    }

    def cleanup() {
        PrimitiveType.explicitObjectType = false
    }

    void "test described POJO property doesn't leak redundant object schema into allOf"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

class MyDto {

    @Schema(description = "the parameters description")
    private Parameters parameters;

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}

class Parameters {

    private Integer stampWidth;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openApi = Utils.testReference
        var dtoSchema = openApi.components.schemas['MyDto']

        then: "the described POJO property keeps only the \$ref, without a redundant object schema"
        dtoSchema != null
        dtoSchema.properties.parameters.description == 'the parameters description'
        dtoSchema.properties.parameters.allOf
        dtoSchema.properties.parameters.allOf.size() == 1
        dtoSchema.properties.parameters.allOf[0].$ref == "#/components/schemas/Parameters"
    }
}
