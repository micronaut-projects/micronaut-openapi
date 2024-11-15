package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

class OpenApiMapSchemaSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI doc for map properties"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.micronaut.core.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a pet.
 */
@Introspected
abstract class Pet {

    private Map<String, String> map;

    private Map<String, Pet> complexMap;

    public Pet(Map<String, String> map, Map<String, Pet> complexMap) {
        this.map = map;
        this.complexMap = complexMap;
    }

    public Pet() {
    }

    public Map<String, String> getMap() {
        return map;
    }

    public Map<String, Pet> getComplexMap() {
        return complexMap;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public void setComplexMap(Map<String, Pet> complexMap) {
        this.complexMap = complexMap;
    }
}

@Controller("/pets")
class PetController {

    @Operation(summary = "Save a pet", description = "The saved pet information is returned")
    @Tag(name = "save-pet")
    @Post
    HttpResponse<Pet> save(@Valid @NotNull @Body Pet pet) {
        return HttpResponse.ok(pet);
    }
}

''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema petSchema = openAPI.components.schemas['Pet']

        then: "the components are valid"
        petSchema != null

        petSchema instanceof Schema

        petSchema.type == 'object'
        petSchema.properties.size() == 2
        petSchema.properties['map'] instanceof MapSchema
        petSchema.properties['complexMap'] instanceof MapSchema

        when:
        MapSchema map = (MapSchema) petSchema.properties['map']
        MapSchema complexMap = (MapSchema) petSchema.properties['complexMap']

        then:
        map.type == 'object'
        map.additionalProperties instanceof StringSchema
        complexMap.type == 'object'
        complexMap.additionalProperties instanceof Schema
        complexMap.additionalProperties['$ref'].contains("Pet")
    }
}
