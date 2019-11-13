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
        def requestBody = operation.requestBody
        def schema = requestBody.content['application/json'].schema
        requestBody.required
        schema.oneOf[0].$ref == '#/components/schemas/A'
        schema.oneOf[1].$ref == '#/components/schemas/B'
        schema.type == 'object'
        schema.discriminator.propertyName == 'type'
        schema.discriminator.mapping['A'] == '#/components/schemas/A'
        schema.discriminator.mapping['B'] == '#/components/schemas/B'

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['A'].properties['age1'].type == 'integer'
        openAPI.components.schemas['A'].allOf[0].$ref == '#/components/schemas/Base'
        openAPI.components.schemas['B'].properties['age2'].type == 'integer'
        openAPI.components.schemas['B'].allOf[0].$ref == '#/components/schemas/Base'
        openAPI.components.schemas['Base'].properties['money'].type == 'integer'
    }

}
