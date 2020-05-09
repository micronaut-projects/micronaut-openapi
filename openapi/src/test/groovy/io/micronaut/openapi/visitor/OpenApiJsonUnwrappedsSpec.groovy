/*
 * Copyright 2017-2020 original authors
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
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Specification

class OpenApiJsonUnwrappedsSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "")
    }

    void "test JsonUnwrapped annotation"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import com.fasterxml.jackson.annotation.*;

@Controller("/test")
interface TestOperations {

    @Post("/")
    Single<Test> save(String name, int age);
}

@Schema
class Dummy {
    public int aa;
    public String bb;
}

@Schema(description = "Represents a pet")
class Pet {
    @Schema(name="pet-name", description = "The pet name")
    private String name;
    private Integer age;

    @Schema(name="pet-name", description = "The pet name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Schema(name="pet-age", description = "The pet age")
    public Integer getAge() {
        return age;
    }
}

@Schema
class Test {
    public Dummy plain;

    @JsonUnwrapped
    public Dummy unwrapped;

    @JsonUnwrapped(prefix = "aaa", suffix = "zzz")
    public Dummy unwrappedRenamed;

    @JsonUnwrapped(enabled = false)
    public Dummy unwrappedDisabled;

    @JsonUnwrapped
    public Pet pet;

    @JsonUnwrapped(prefix = "aaa", suffix = "zzz")
    public Pet petRenamed;
}

@javax.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Schema schema = openAPI.components.schemas['Test']

        then:"the components are valid"
        schema.type == 'object'
        schema.properties.size() == 10
        schema.properties['plain'].$ref == '#/components/schemas/Dummy'
        schema.properties['unwrappedDisabled'].$ref == '#/components/schemas/Dummy'
        schema.properties['aa'].type == 'integer'
        schema.properties['bb'].type == 'string'
        schema.properties['aaaaazzz'].type == 'integer'
        schema.properties['aaabbzzz'].type == 'string'
        schema.properties['pet-name'].type == 'string'
        schema.properties['pet-age'].type == 'integer'
        schema.properties['aaapet-namezzz'].type == 'string'
        schema.properties['aaapet-agezzz'].type == 'integer'
        !schema.required
    }
}
