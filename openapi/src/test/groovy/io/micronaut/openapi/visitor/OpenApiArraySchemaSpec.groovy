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
import io.micronaut.http.MediaType
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiArraySchemaSpec extends AbstractTypeElementSpec {
    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    void "test ArraySchema with arraySchema field in class"() {
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

    @Get("/")
    public Pets findPets() {
        return null;
    }
}

@Schema(description = "Pets")
class Pets {
    @ArraySchema(arraySchema = @Schema(description = "a list of Pets", nullable = false), minItems = 2, schema = @Schema(description = "No", implementation = Pet.class, nullable = true))
    public List<Pet> pets;

    @ArraySchema(arraySchema = @Schema(description = "a list of Ids", nullable = false), minItems = 2, schema = @Schema(description = "Yes", nullable = true))
    public List<Long> ids;

    @ArraySchema(arraySchema = @Schema(description = "a list of primitive Ids", nullable = false), minItems = 2, schema = @Schema(description = "Yes", nullable = true))
    public long[] primitiveIds;
}

@Schema(description = "Pet")
class Pet {
    @Schema(description = "The name of the pet")
    public String name;
}

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.get

        expect:
        operation
        operation.responses.size() == 1
        openAPI.components.schemas['Pets'].description == 'Pets'
        openAPI.components.schemas['Pets'].properties['pets'].nullable == false
        openAPI.components.schemas['Pets'].properties['pets'].description == 'a list of Pets'
        openAPI.components.schemas['Pets'].properties['pets'].minItems == 2
        openAPI.components.schemas['Pets'].properties['pets'].items.$ref == '#/components/schemas/Pet'
        openAPI.components.schemas['Pets'].properties['pets'].items.description == null
        openAPI.components.schemas['Pets'].properties['pets'].items.nullable == null

        openAPI.components.schemas['Pets'].properties['ids'].nullable == false
        openAPI.components.schemas['Pets'].properties['ids'].description == 'a list of Ids'
        openAPI.components.schemas['Pets'].properties['ids'].minItems == 2
        openAPI.components.schemas['Pets'].properties['ids'].items.format == 'int64'
        openAPI.components.schemas['Pets'].properties['ids'].items.description == 'Yes'
        openAPI.components.schemas['Pets'].properties['ids'].items.nullable == true

        openAPI.components.schemas['Pets'].properties['primitiveIds'].nullable == false
        openAPI.components.schemas['Pets'].properties['primitiveIds'].description == 'a list of primitive Ids'
        openAPI.components.schemas['Pets'].properties['primitiveIds'].minItems == 2
        openAPI.components.schemas['Pets'].properties['primitiveIds'].items.format == 'int64'
        openAPI.components.schemas['Pets'].properties['primitiveIds'].items.description == 'Yes'
        openAPI.components.schemas['Pets'].properties['primitiveIds'].items.nullable == true
    }

    void "test ArraySchema with arraySchema field in Controller ApiResponse"() {
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
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Get("/")
    @Operation(description = "Lists the Pets.")
    @ApiResponse(responseCode = "200", description = "Returns a list of _Pet_s.", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(minItems = 2, arraySchema = @Schema(description = "A list of Pets", example = "[{'name': 'cat'}, {'name': 'dog'}]"), schema = @Schema(implementation = Pet.class))))
    public List<Pet> findPets() {
        return null;
    }
}

@Schema(description = "Pet")
class Pet {
    @Schema(description = "The name of the pet")
    public String name;
}

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.get

        expect:
        operation
        operation.responses.size() == 1
        operation.responses.'200'.content.'application/json'.schema.description == 'A list of Pets'
        operation.responses.'200'.content.'application/json'.schema.minItems == 2
        operation.responses.'200'.content.'application/json'.schema.items.$ref ==  '#/components/schemas/Pet'

    }

    void "test ArraySchema with arraySchema field in Controller Parameter"() {
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
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Get("/{?names*}")
    @Operation(description = "Lists the Pets.")
    @ApiResponse(responseCode = "200", description = "Returns a list of _Pet_s.", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(minItems = 2, arraySchema = @Schema(description = "A list of Pets", example = "[{'name': 'cat'}, {'name': 'dog'}]"), schema = @Schema(implementation = Pet.class))))
    public List<Pet> findPets(@Parameter(in = ParameterIn.QUERY, required = true, description = "A list of names", example = "['dog', 'cat']", array = @ArraySchema(minItems = 2, arraySchema = @Schema(description = "A list of _Pet_'s name"), schema = @Schema(type = "string"))) List<String> names) {
        return null;
    }
}

@Schema(description = "Pet")
class Pet {
    @Schema(description = "The name of the pet")
    public String name;
}

@javax.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/")?.get

        expect:
        operation
        operation.responses.size() == 1
        operation.responses.'200'.content.'application/json'.schema.description == 'A list of Pets'
        operation.responses.'200'.content.'application/json'.schema.minItems == 2
        operation.responses.'200'.content.'application/json'.schema.items.$ref ==  '#/components/schemas/Pet'

        operation.parameters
        operation.parameters.size() == 1
        operation.parameters[0].schema.description == 'A list of _Pet_\'s name'
        operation.parameters[0].schema.minItems == 2
        operation.parameters[0].schema.items.type ==  'string'
    }
}