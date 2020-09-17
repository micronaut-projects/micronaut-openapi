
package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.RequestBody

class OpenApiSchemaInheritanceSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    void "test parse the OpenAPI with response that contains generic types"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Put("/")
    @Operation(summary = "Update an existing pet")
        @RequestBody(required = true, content = @Content(
            schema = @Schema(
                    oneOf = {
                            A.class, B.class
                    },
                    discriminatorMapping = {
                            @DiscriminatorMapping(value = "A", schema = A.class),
                            @DiscriminatorMapping(value = "B", schema = B.class)
                    },
                    discriminatorProperty = "type")
    ))
    public void updatePet(Base base) {
    }
}

abstract class Base {
    private int money;

    public void setMoney(int a) {
        money = a;
    }

    public int getMoney() {
        return money;
    }
}

class A extends Base {
    private int age1;

    public void setAge1(int a) {
        age1 = a;
    }

    public int getAge1() {
        return age1;
    }
}

class B extends Base {
    private int age2;

    public void setAge2(int a) {
        age2 = a;
    }

    public int getAge2() {
        return age2;
    }
}

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.put
        RequestBody requestBody = operation.requestBody
        requestBody.required
        Schema schema = requestBody.content['application/json'].schema
        schema instanceof ComposedSchema
        ((ComposedSchema) schema).oneOf[0].$ref == '#/components/schemas/A'
        ((ComposedSchema) schema).oneOf[1].$ref == '#/components/schemas/B'
        ((ComposedSchema) schema).type == 'object'
        ((ComposedSchema) schema).discriminator.propertyName == 'type'
        ((ComposedSchema) schema).discriminator.mapping['A'] == '#/components/schemas/A'
        ((ComposedSchema) schema).discriminator.mapping['B'] == '#/components/schemas/B'

        expect:
        operation
        operation.responses.size() == 1
        operation.responses["200"].description == "updatePet 200 response"
        operation.responses["200"].content == null

        openAPI.components.schemas['Base'].properties['money'].type == 'integer'

        openAPI.components.schemas['A'] instanceof ComposedSchema
        ((ComposedSchema) openAPI.components.schemas['A']).allOf.size() == 2
        ((ComposedSchema) openAPI.components.schemas['A']).allOf[0].get$ref() == "#/components/schemas/Base"
        ((ComposedSchema) openAPI.components.schemas['A']).allOf[1].properties.size() == 1
        ((ComposedSchema) openAPI.components.schemas['A']).allOf[1].properties['age1'].type == "integer"

        openAPI.components.schemas['B'] instanceof ComposedSchema
        ((ComposedSchema) openAPI.components.schemas['B']).allOf.size() == 2
        ((ComposedSchema) openAPI.components.schemas['B']).allOf[0].get$ref() == "#/components/schemas/Base"
        ((ComposedSchema) openAPI.components.schemas['B']).allOf[1].properties.size() == 1
        ((ComposedSchema) openAPI.components.schemas['B']).allOf[1].properties["age2"].type == "integer"
    }

}
