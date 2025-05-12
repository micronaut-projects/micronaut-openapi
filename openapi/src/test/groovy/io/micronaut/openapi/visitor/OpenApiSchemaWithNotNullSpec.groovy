package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Issue

class OpenApiSchemaWithNotNullSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI doc for @Schema with @NotNull"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Controller("/path")
class OpenApiController {

    @Post(consumes = MediaType.APPLICATION_JSON)
    @Operation(summary = "OpenAPI example")
    @Status(HttpStatus.NO_CONTENT)
    public void processSync(@Valid @Body Dto dto) {
    }
}

@Schema(description = "Data Transfer Object")
class Dto {
    @Schema(description = "Description set through @Schema annotation. Causes attribute not being marked as required",
            maxLength = 8,
            example = "526630")
    @NotNull
    @PositiveOrZero
    @Max(99999999)
    Integer id;

    /**
     * Description set through Javadoc. Everything is correct but adding examples is not possible.
     */
    @NotNull
    @PositiveOrZero
    @Max(99999999)
    Integer idJavadoc;

    public Integer getId() {
        return id;
    }

    public Integer getIdJavadoc() {
        return idJavadoc;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths."/path".post
        Schema dtoSchema = openAPI.components.schemas['Dto']

        then:
        operation
        operation.operationId == "processSync"
        operation.requestBody.content."application/json".schema.$ref == "#/components/schemas/Dto"

        dtoSchema
        dtoSchema.description == 'Data Transfer Object'
        dtoSchema.required.size() == 2
        dtoSchema.required.contains("id")
        dtoSchema.required.contains("idJavadoc")
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/2116")
    void "test build OpenAPI spec with @NonNull / @Nullable / simple properties"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/hello")
class HelloController {

    @Get
    public HelloClass hello() {
        return new HelloClass("Not Null Value", "Nullable", "Plain Value");
    }
}

class HelloClass {

    @NonNull
    private String notNullProperty;
    @Nullable
    private String nullableProperty;
    private String plainProperty;

    public HelloClass(@NonNull String notNullProperty, @Nullable String nullableProperty, String plainProperty) {
        this.notNullProperty = notNullProperty;
        this.nullableProperty = nullableProperty;
        this.plainProperty = plainProperty;
    }

    @NonNull
    public String getNotNullProperty() {
        return notNullProperty;
    }

    public void setNotNullProperty(@NonNull String notNullProperty) {
        this.notNullProperty = notNullProperty;
    }

    @Nullable
    public String getNullableProperty() {
        return nullableProperty;
    }

    public void setNullableProperty(@Nullable String nullableProperty) {
        this.nullableProperty = nullableProperty;
    }

    public String getPlainProperty() {
        return plainProperty;
    }

    public void setPlainProperty(String plainProperty) {
        this.plainProperty = plainProperty;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths."/hello".get
        Schema dtoSchema = openAPI.components.schemas.HelloClass

        then:
        operation
        dtoSchema
        dtoSchema.required.size() == 1
        dtoSchema.required.contains("notNullProperty")
        dtoSchema.properties.nullableProperty.nullable
    }
}
