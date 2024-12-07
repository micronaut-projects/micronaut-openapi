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
    @ApiResponse(responseCode = "200", description = "Returns a list of _Pet_s.", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(minItems = 2, arraySchema = @Schema(description = "A list of Pets", example = "[{\\"name\\": \\"cat\\"}, {\\"name\\": \\"dog\\"}]"), schema = @Schema(implementation = Pet.class))))
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
    @ApiResponse(responseCode = "200", description = "Returns a list of _Pet_s.", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(minItems = 2, arraySchema = @Schema(description = "A list of Pets", example = "[{\\"name\\": \\"cat\\"}, {\\"name\\": \\"dog\\"}]"), schema = @Schema(implementation = Pet.class))))
    public List<Pet> findPets(@Parameter(in = ParameterIn.QUERY, required = true, description = "A list of names", example = "[\\"dog\\", \\"cat\\"]", array = @ArraySchema(minItems = 2, arraySchema = @Schema(description = "A list of _Pet_'s name"), schema = @Schema(type = "string"))) List<String> names) {
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

    void "test example for array schema"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Controller
class MyController {

    @Post("/names")
    public List<Pet> findPets(@Body List<Pet> pets) {
        return null;
    }
}

@Schema(description = "Pet")
class Pet {
    @Schema(description = "The name of the pet", example = "[\\"123 Main St\\", \\"Suite 517\\"]")
    public List<String> lines;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        def petSchema = openAPI.components?.schemas?.Pet

        expect:
        petSchema
        petSchema.properties
        petSchema.properties.lines.example instanceof List<String>
        petSchema.properties.lines.example[0] == '123 Main St'
        petSchema.properties.lines.example[1] == 'Suite 517'
    }

    void "test ArraySchema and schema extended ArrayList"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Controller
interface DefaultApi {

    @Get("/hello")
    Teste hello();
}

@Schema(name = "Teste")
class Teste {

    @ArraySchema(schema = @Schema(name = "array", example = "[\\"Any Example\\"]", implementation = List.class, requiredMode = Schema.RequiredMode.NOT_REQUIRED))
    public List<String> array = new ArrayList<>();

    @ArraySchema(schema = @Schema(name = "array2", example = "[\\"Any Example\\",\\"Other Example\\"]", implementation = List.class, requiredMode = Schema.RequiredMode.NOT_REQUIRED))
    @NotNull
    public List<String> array2 = new ArrayList<>();

    @ArraySchema(schema = @Schema(name = "array3", implementation = LastRetryRecurringPaymentIds.class, requiredMode = Schema.RequiredMode.NOT_REQUIRED))
    public List<LastRetryRecurringPaymentIds> array3 = new ArrayList<>();
}

@ArraySchema(arraySchema = @Schema(name = "LastRetryRecurringPaymentIds", example = "[\\"any example\\"]"))
class LastRetryRecurringPaymentIds extends ArrayList<String> {
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference
        def schemas = openApi.components.schemas

        then:
        schemas.Teste
        schemas.Teste.required.size() == 1
        schemas.Teste.required[0] == "array2"
        schemas.Teste.properties.size() == 3

        schemas.Teste.properties.array.type == "array"
        schemas.Teste.properties.array.items.type == "string"
        schemas.Teste.properties.array.items.example == "[\"Any Example\"]"

        schemas.Teste.properties.array2.type == "array"
        schemas.Teste.properties.array2.items.type == "string"
        schemas.Teste.properties.array2.items.example == "[\"Any Example\",\"Other Example\"]"

        schemas.Teste.properties.array3.type == "array"
        schemas.Teste.properties.array3.items.type == "array"
        schemas.Teste.properties.array3.items.example instanceof List
        ((List<String>) schemas.Teste.properties.array3.items.example)[0] == "any example"
        schemas.Teste.properties.array3.items.items.$ref == "#/components/schemas/LastRetryRecurringPaymentIds"

        schemas.LastRetryRecurringPaymentIds
        schemas.LastRetryRecurringPaymentIds.type == "array"
        schemas.LastRetryRecurringPaymentIds.example instanceof List
        ((List<String>) schemas.LastRetryRecurringPaymentIds.example)[0] == "any example"
        schemas.LastRetryRecurringPaymentIds.items.type == "string"
    }
}
