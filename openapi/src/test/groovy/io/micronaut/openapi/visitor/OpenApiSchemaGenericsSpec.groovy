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

class OpenApiSchemaGenericsSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    void "test parse OpenAPI with recursive generics"() {
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
    public Pet updatePet(Pet pet) {
        return null;
    }
}

class Pet implements java.util.function.Consumer<Pet> {
    private PetType type;


    public void setType(PetType type) {
        this.type = type;
    }

    /**
     * The age
     */
    public PetType getType() {
        return this.type;
    }

    public void accept(Pet pet) {}
}

enum PetType {
    DOG, CAT;
}

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['Pet']
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
    public Response<Pet> updatePet(Pet pet) {
        return null;
    }
}

class Pet {
    @javax.validation.constraints.Min(18)
    private int age;


    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    public int getAge() {
        return age;
    }
}

class Response<T> {
    T r;
    public T getResult() {
        return r;
    };
}

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['Pet'].properties['age'].type == 'integer'
        openAPI.components.schemas['Response<Pet>'].properties['result'].$ref == '#/components/schemas/Pet'
    }



    void "test parse the OpenAPI with response that contains nested generic types"() {
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
    public Response<List<Pet>> updatePet(@Body Response<List<Pet>> pet) {
        return null;
    }
}

class Pet {
    @javax.validation.constraints.Min(18)
    private int age;


    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    public int getAge() {
        return age;
    }
}

class Response<T> {
    T r;
    public T getResult() {
        return r;
    };
}

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['Pet'].properties['age'].type == 'integer'
        openAPI.components.schemas['Response<List<Pet>>'].properties['result'].type == 'array'
        openAPI.components.schemas['Response<List<Pet>>'].properties['result'].items.$ref == '#/components/schemas/Pet'
    }

    void "test parse the OpenAPI with response that contains generic types and custom schema name"() {
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
    @Schema(name="ResponseOfPet")
    public Response<Pet> updatePet(@Body @Schema(name="RequestOfPet") Response<Pet> pet) {
        return null;
    }
}

class Pet {
    @javax.validation.constraints.Min(18)
    private int age;


    public void setAge(int a) {
        age = a;
    }

    /**
     * The age
     */
    public int getAge() {
        return age;
    }
}

class Response<T> {
    T r;
    public T getResult() {
        return r;
    };
}

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.put

        expect:
        operation
        operation.responses.size() == 1
        operation.responses.default.content['application/json'].schema.$ref == '#/components/schemas/ResponseOfPet'
        operation.requestBody.content['application/json'].schema.$ref == '#/components/schemas/RequestOfPet'
        openAPI.components.schemas['Pet'].properties['age'].type == 'integer'
        openAPI.components.schemas['ResponseOfPet'].properties['result'].$ref == '#/components/schemas/Pet'
        openAPI.components.schemas['RequestOfPet'].properties['result'].$ref == '#/components/schemas/Pet'
    }
}
