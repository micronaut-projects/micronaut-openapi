package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiArraySchemaSpec extends AbstractOpenApiTypeElementSpec {

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

    @ArraySchema(arraySchema = @Schema(description = "a nested array of primitive Ids"))
    public long[][] nestedPrimitiveIds;

    @ArraySchema(arraySchema = @Schema(description = "a nested list of Pets"))
    public List<List<Pet>> nestedPetList;

    @ArraySchema(arraySchema = @Schema(description = "a nested array of Pets"))
    public Pet[][] nestedPetArray;

    @ArraySchema(arraySchema = @Schema(description = "a nested array of Ids"))
    public Long[][] nestedIdArray;

    @ArraySchema(arraySchema = @Schema(description = "a list of nested Ids"))
    public List<Long[]> idArrayList;

    @ArraySchema(arraySchema = @Schema(description = "an array of nested Ids"))
    public List<Long>[] idListArray;
}

@Schema(description = "Pet")
class Pet {
    @Schema(description = "The name of the pet")
    public String name;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/")?.get
        def petSchema = openAPI.components.schemas['Pets'];

        expect:
        operation
        operation.responses.size() == 1
        petSchema.description == 'Pets'
        petSchema.properties['pets'].nullable == false
        petSchema.properties['pets'].description == 'a list of Pets'
        petSchema.properties['pets'].minItems == 2
        petSchema.properties['pets'].items.$ref == '#/components/schemas/Pet'
        petSchema.properties['pets'].items.nullable == null

        petSchema.properties['ids'].nullable == false
        petSchema.properties['ids'].description == 'a list of Ids'
        petSchema.properties['ids'].minItems == 2
        petSchema.properties['ids'].items.format == 'int64'
        petSchema.properties['ids'].items.description == 'Yes'
        petSchema.properties['ids'].items.nullable == true

        petSchema.properties['primitiveIds'].nullable == false
        petSchema.properties['primitiveIds'].description == 'a list of primitive Ids'
        petSchema.properties['primitiveIds'].minItems == 2
        petSchema.properties['primitiveIds'].items.format == 'int64'
        petSchema.properties['primitiveIds'].items.description == 'Yes'
        petSchema.properties['primitiveIds'].items.nullable == true

        petSchema.properties['nestedPrimitiveIds'].description == 'a nested array of primitive Ids'
        petSchema.properties['nestedPrimitiveIds'].items.items.format == 'int64'

        petSchema.properties['nestedPetList'].description == 'a nested list of Pets'
        petSchema.properties['nestedPetList'].items.items.$ref == '#/components/schemas/Pet'

        petSchema.properties['nestedPetArray'].description == 'a nested array of Pets'
        petSchema.properties['nestedPetArray'].items.items.$ref == '#/components/schemas/Pet'

        petSchema.properties['nestedIdArray'].description == 'a nested array of Ids'
        petSchema.properties['nestedIdArray'].items.items.format == 'int64'

        petSchema.properties['idArrayList'].description == 'a list of nested Ids'
        petSchema.properties['idArrayList'].items.items.format == 'int64'

        petSchema.properties['idListArray'].description == 'an array of nested Ids'
        petSchema.properties['idListArray'].items.items.format == 'int64'
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

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
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

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
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
