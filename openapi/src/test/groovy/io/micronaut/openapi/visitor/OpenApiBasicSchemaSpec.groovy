package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Issue

import java.time.OffsetDateTime

class OpenApiBasicSchemaSpec extends AbstractOpenApiTypeElementSpec {

    void "test @PositiveOrZero and @NegativeOrZero correctly results in minimum 0 and maximum 0"() {

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.constraints.*;

@Controller
class PersonController {

    @Operation(
            summary = "Fetch the person information",
            description = "Fetch the person name, debt and goals information",
            parameters = { @Parameter(name = "name", required = true, description = "The person name", in = ParameterIn.PATH) },
            responses = {
                    @ApiResponse(description = "The person information",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema( implementation = Person.class )
                    ))
            }
    )
    @Get("/person/{name}")
    HttpResponse<Person> get(@NotBlank String name) {
        return HttpResponse.ok();
    }
}

/**
 * The person information.
 */
@Introspected
class Person {

    @NotBlank
    private String name;

    @NegativeOrZero
    private Integer debtValue;

    @PositiveOrZero
    private Integer totalGoals;

    public Person(@NotBlank String name,
                  @NegativeOrZero Integer debtValue,
                  @PositiveOrZero Integer totalGoals) {

        this.name = name;
        this.debtValue = debtValue;
        this.totalGoals = totalGoals;
    }

     /**
     * The person full name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

     /**
     * The total debt amount.
     *
     * @return The debtValue
     */
    public Integer getDebtValue() {
        return debtValue;
    }

     /**
     * The total number of person's goals.
     *
     * @return The totalGoals
     */
    public Integer getTotalGoals() {
        return totalGoals;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDebtValue(Integer debtValue) {
        this.debtValue = debtValue;
    }

    public void setTotalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals;
    }
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        openAPI?.paths?.get("/person/{name}")?.get
        openAPI.components.schemas["Person"]
        openAPI.components.schemas["Person"].type == "object"

        openAPI.components.schemas["Person"].properties
        openAPI.components.schemas["Person"].properties.size() == 3

        openAPI.components.schemas["Person"].properties["name"]
        openAPI.components.schemas["Person"].properties["debtValue"]
        openAPI.components.schemas["Person"].properties["totalGoals"]

        openAPI.components.schemas["Person"].properties["name"].type == "string"
        openAPI.components.schemas["Person"].properties["name"].description == "The person full name."

        openAPI.components.schemas["Person"].properties["debtValue"].type == "integer"
        openAPI.components.schemas["Person"].properties["debtValue"].maximum == 0
        !openAPI.components.schemas["Person"].properties["debtValue"].exclusiveMaximum
        openAPI.components.schemas["Person"].properties["debtValue"].description == "The total debt amount."

        openAPI.components.schemas["Person"].properties["totalGoals"].type == "integer"
        !openAPI.components.schemas["Person"].properties["totalGoals"].exclusiveMinimum
        openAPI.components.schemas["Person"].properties["totalGoals"].description == "The total number of person's goals."
    }

    void "test schema with fluent accessors"() {

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.core.annotation.AccessorsStyle;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.constraints.*;

@Controller
class PersonController {

    @Operation(
            summary = "Fetch the person information",
            description = "Fetch the person name, debt and goals information",
            parameters = { @Parameter(name = "name", required = true, description = "The person name", in = ParameterIn.PATH) },
            responses = {
                    @ApiResponse(description = "The person information",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema( implementation = Person.class )
                    ))
            }
    )
    @Get("/person/{name}")
    HttpResponse<Person> get(@NotBlank String name) {
        return HttpResponse.ok();
    }
}

/**
 * The person information.
 */
@Introspected
@AccessorsStyle(readPrefixes = "", writePrefixes = "")
class Person {

    @NotBlank
    private String name;

    @NegativeOrZero
    private Integer debtValue;

    @PositiveOrZero
    private Integer totalGoals;

    public Person(@NotBlank String name,
                  @NegativeOrZero Integer debtValue,
                  @PositiveOrZero Integer totalGoals) {

        this.name = name;
        this.debtValue = debtValue;
        this.totalGoals = totalGoals;
    }

     /**
     * The person full name.
     *
     * @return The name
     */
    public String name() {
        return name;
    }

     /**
     * The total debt amount.
     *
     * @return The debtValue
     */
    public Integer debtValue() {
        return debtValue;
    }

     /**
     * The total number of person's goals.
     *
     * @return The totalGoals
     */
    public Integer totalGoals() {
        return totalGoals;
    }

    public void name(String name) {
        this.name = name;
    }

    public void debtValue(Integer debtValue) {
        this.debtValue = debtValue;
    }

    public void totalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals;
    }
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        openAPI?.paths?.get("/person/{name}")?.get
        openAPI.components.schemas["Person"]
        openAPI.components.schemas["Person"].type == "object"

        openAPI.components.schemas["Person"].properties
        openAPI.components.schemas["Person"].properties.size() == 3

        openAPI.components.schemas["Person"].properties["name"]
        openAPI.components.schemas["Person"].properties["debtValue"]
        openAPI.components.schemas["Person"].properties["totalGoals"]

        openAPI.components.schemas["Person"].properties["name"].type == "string"
        openAPI.components.schemas["Person"].properties["name"].description == "The person full name."

        openAPI.components.schemas["Person"].properties["debtValue"].type == "integer"
        openAPI.components.schemas["Person"].properties["debtValue"].maximum == 0
        !openAPI.components.schemas["Person"].properties["debtValue"].exclusiveMaximum
        openAPI.components.schemas["Person"].properties["debtValue"].description == "The total debt amount."

        openAPI.components.schemas["Person"].properties["totalGoals"].type == "integer"
        !openAPI.components.schemas["Person"].properties["totalGoals"].exclusiveMinimum
        openAPI.components.schemas["Person"].properties["totalGoals"].description == "The total number of person's goals."
    }

    void "test @Pattern in Schema"() {

        when:
        buildBeanDefinition("test.MyBean", '''
package test;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.*;
@Controller
class PersonController {
    @Operation(
            summary = "Fetch the person information",
            description = "Fetch the person name, debt and goals information",
            parameters = { @Parameter(name = "name", required = true, description = "The person name", in = ParameterIn.PATH) },
            responses = {
                    @ApiResponse(description = "The person information",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema( implementation = Person.class )
                    ))
            }
    )
    @Get("/person/{name}")
    HttpResponse<Person> get(@NotBlank String name) {
        return HttpResponse.ok();
    }
}
/**
 * The person information.
 */
@Introspected
class Person {
    @NotBlank
    @Pattern(regexp = "xxxx")
    private String name;
    @NegativeOrZero
    private Integer debtValue;
    @PositiveOrZero
    private Integer totalGoals;
    public Person(@NotBlank String name,
                  @NegativeOrZero Integer debtValue,
                  @PositiveOrZero Integer totalGoals) {
        this.name = name;
        this.debtValue = debtValue;
        this.totalGoals = totalGoals;
    }
     /**
     * The person full name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }
     /**
     * The total debt amount.
     *
     * @return The debtValue
     */
    public Integer getDebtValue() {
        return debtValue;
    }
     /**
     * The total number of person's goals.
     *
     * @return The totalGoals
     */
    public Integer getTotalGoals() {
        return totalGoals;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setDebtValue(Integer debtValue) {
        this.debtValue = debtValue;
    }
    public void setTotalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals;
    }
}
@jakarta.inject.Singleton
public class MyBean {}
''')

        then:
        OpenAPI openAPI = Utils.testReference
        openAPI?.paths?.get("/person/{name}")?.get
        openAPI.components.schemas["Person"]
        openAPI.components.schemas["Person"].type == "object"

        openAPI.components.schemas["Person"].properties
        openAPI.components.schemas["Person"].properties.size() == 3

        openAPI.components.schemas["Person"].properties["name"]
        openAPI.components.schemas["Person"].properties["debtValue"]
        openAPI.components.schemas["Person"].properties["totalGoals"]

        openAPI.components.schemas["Person"].properties["name"].type == "string"
        openAPI.components.schemas["Person"].properties["name"].description == "The person full name."
        openAPI.components.schemas["Person"].properties["name"].pattern == "xxxx"


        openAPI.components.schemas["Person"].properties["debtValue"].type == "integer"
        openAPI.components.schemas["Person"].properties["debtValue"].maximum == 0
        !openAPI.components.schemas["Person"].properties["debtValue"].exclusiveMaximum
        openAPI.components.schemas["Person"].properties["debtValue"].description == "The total debt amount."

        openAPI.components.schemas["Person"].properties["totalGoals"].type == "integer"
        !openAPI.components.schemas["Person"].properties["totalGoals"].exclusiveMinimum
        openAPI.components.schemas["Person"].properties["totalGoals"].description == "The total number of person's goals."
    }

    void "test render OpenApiView specification with custom property naming strategy"() {
        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY, "SNAKE_CASE")

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.constraints.*;

@Controller
class PersonController {

    @Operation(
            summary = "Fetch the person information",
            description = "Fetch the person name, debt and goals information",
            parameters = { @Parameter(name = "name", required = true, description = "The person name", in = ParameterIn.PATH) },
            responses = {
                    @ApiResponse(description = "The person information",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema( implementation = Person.class )
                    ))
            }
    )
    @Get("/person/{name}")
    HttpResponse<Person> get(@NotBlank String name) {
        return HttpResponse.ok();
    }
}

/**
 * The person information.
 */
@Introspected
class Person {

    @NotBlank
    private String name;

    @NegativeOrZero
    private Integer debtValue;

    @PositiveOrZero
    private Integer totalGoals;

    public Person(@NotBlank String name,
                  @NegativeOrZero Integer debtValue,
                  @PositiveOrZero Integer totalGoals) {

        this.name = name;
        this.debtValue = debtValue;
        this.totalGoals = totalGoals;
    }

     /**
     * The person full name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

     /**
     * The total debt amount.
     *
     * @return The debtValue
     */
    public Integer getDebtValue() {
        return debtValue;
    }

     /**
     * The total number of person's goals.
     *
     * @return The totalGoals
     */
    public Integer getTotalGoals() {
        return totalGoals;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDebtValue(Integer debtValue) {
        this.debtValue = debtValue;
    }

    public void setTotalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals;
    }
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        openAPI?.paths?.get("/person/{name}")?.get
        openAPI.components.schemas["Person"]
        openAPI.components.schemas["Person"].type == "object"

        openAPI.components.schemas["Person"].properties
        openAPI.components.schemas["Person"].properties.size() == 3

        openAPI.components.schemas["Person"].properties["name"]
        openAPI.components.schemas["Person"].properties["debt_value"]
        openAPI.components.schemas["Person"].properties["total_goals"]

        openAPI.components.schemas["Person"].properties["name"].type == "string"
        openAPI.components.schemas["Person"].properties["name"].description == "The person full name."

        openAPI.components.schemas["Person"].properties["debt_value"].type == "integer"
        openAPI.components.schemas["Person"].properties["debt_value"].maximum == 0
        !openAPI.components.schemas["Person"].properties["debt_value"].exclusiveMaximum
        openAPI.components.schemas["Person"].properties["debt_value"].description == "The total debt amount."

        openAPI.components.schemas["Person"].properties["total_goals"].type == "integer"
        !openAPI.components.schemas["Person"].properties["total_goals"].exclusiveMinimum
        openAPI.components.schemas["Person"].properties["total_goals"].description == "The total number of person's goals."

        cleanup:
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY)
    }

    void "test render OpenApiView specification with LOWER_CAMEL_CASE property naming strategy - Issue #241"() {
        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY, "LOWER_CAMEL_CASE")

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.constraints.*;

@Controller
class PersonController {

    @Operation(
            summary = "Fetch the person information",
            description = "Fetch the person name, debt and goals information",
            parameters = { @Parameter(name = "name", required = true, description = "The person name", in = ParameterIn.PATH) },
            responses = {
                    @ApiResponse(description = "The person information",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema( implementation = Person.class )
                    ))
            }
    )
    @Get("/person/{name}")
    HttpResponse<Person> get(@NotBlank String name) {
        return HttpResponse.ok();
    }
}

/**
 * The person information.
 */
@Introspected
class Person {

    @NotBlank
    private String name;

    @NegativeOrZero
    private Integer debtValue;

    @PositiveOrZero
    private Integer totalGoals;

    public Person(@NotBlank String name,
                  @NegativeOrZero Integer debtValue,
                  @PositiveOrZero Integer totalGoals) {

        this.name = name;
        this.debtValue = debtValue;
        this.totalGoals = totalGoals;
    }

     /**
     * The person full name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

     /**
     * The total debt amount.
     *
     * @return The debtValue
     */
    public Integer getDebtValue() {
        return debtValue;
    }

     /**
     * The total number of person's goals.
     *
     * @return The totalGoals
     */
    public Integer getTotalGoals() {
        return totalGoals;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDebtValue(Integer debtValue) {
        this.debtValue = debtValue;
    }

    public void setTotalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals;
    }
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        openAPI?.paths?.get("/person/{name}")?.get
        openAPI.components.schemas["Person"]
        openAPI.components.schemas["Person"].type == "object"

        openAPI.components.schemas["Person"].properties
        openAPI.components.schemas["Person"].properties.size() == 3

        openAPI.components.schemas["Person"].properties["name"]
        openAPI.components.schemas["Person"].properties["debtValue"]
        openAPI.components.schemas["Person"].properties["totalGoals"]

        openAPI.components.schemas["Person"].properties["name"].type == "string"
        openAPI.components.schemas["Person"].properties["name"].description == "The person full name."

        openAPI.components.schemas["Person"].properties["debtValue"].type == "integer"
        openAPI.components.schemas["Person"].properties["debtValue"].maximum == 0
        !openAPI.components.schemas["Person"].properties["debtValue"].exclusiveMaximum
        openAPI.components.schemas["Person"].properties["debtValue"].description == "The total debt amount."

        openAPI.components.schemas["Person"].properties["totalGoals"].type == "integer"
        !openAPI.components.schemas["Person"].properties["totalGoals"].exclusiveMinimum
        openAPI.components.schemas["Person"].properties["totalGoals"].description == "The total number of person's goals."

        cleanup:
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY)
    }

    void "test render OpenApiView specification with custom property naming strategy and required properties - Issue #240"() {
        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY, "SNAKE_CASE")

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.constraints.*;

@Controller
class PersonController {

    @Operation(
            summary = "Fetch the person information",
            description = "Fetch the person name, debt and goals information",
            parameters = { @Parameter(name = "name", required = true, description = "The person name", in = ParameterIn.PATH) },
            responses = {
                    @ApiResponse(description = "The person information",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema( implementation = Person.class )
                    ))
            }
    )
    @Get("/person/{name}")
    HttpResponse<Person> get(@NotBlank String name) {
        return HttpResponse.ok();
    }
}

/**
 * The person information.
 */
@Introspected
@Schema(requiredProperties = {"name", "debtValue"})
class Person {

    @NotBlank
    private String name;

    @NegativeOrZero
    private Integer debtValue;

    @PositiveOrZero
    private Integer totalGoals;

    @Email
    @io.swagger.v3.oas.annotations.media.Schema(name = "xyz", implementation = String.class)
    public java.util.Map<String, java.util.List<Integer>> mapValue;

    public Person(@NotBlank String name,
                  @NegativeOrZero Integer debtValue,
                  @PositiveOrZero Integer totalGoals) {

        this.name = name;
        this.debtValue = debtValue;
        this.totalGoals = totalGoals;
    }

     /**
     * The person full name.
     *
     * @return The name
     */
    @Schema()
    public String getName() {
        return name;
    }

     /**
     * The total debt amount.
     *
     * @return The debtValue
     */
    public Integer getDebtValue() {
        return debtValue;
    }

     /**
     * The total number of person's goals.
     *
     * @return The totalGoals
     */
    public Integer getTotalGoals() {
        return totalGoals;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDebtValue(Integer debtValue) {
        this.debtValue = debtValue;
    }

    public void setTotalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals;
    }
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        openAPI?.paths?.get("/person/{name}")?.get
        Schema personSchema = openAPI.components.schemas.Person

        personSchema
        personSchema.type == "object"

        personSchema.properties
        personSchema.properties.size() == 4

        personSchema.properties["name"]
        personSchema.properties["debt_value"]
        personSchema.properties["total_goals"]

        personSchema.properties["name"].type == "string"
        personSchema.properties["name"].description == "The person full name."

        personSchema.properties["debt_value"].type == "integer"
        personSchema.properties["debt_value"].maximum == 0
        !personSchema.properties["debt_value"].exclusiveMaximum
        personSchema.properties["debt_value"].description == "The total debt amount."

        personSchema.properties["total_goals"].type == "integer"
        !personSchema.properties["total_goals"].exclusiveMinimum
        personSchema.properties["total_goals"].description == "The total number of person's goals."

        personSchema.properties["xyz"].type == "string"
        personSchema.properties["xyz"].additionalProperties == null
        personSchema.properties["xyz"].format == "email"

        personSchema.required.size() == 3
        personSchema.required.contains("name")
        personSchema.required.contains("debt_value")
        personSchema.required.contains("total_goals")

        cleanup:
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_PROPERTY_NAMING_STRATEGY)
    }

    void "test READ_ONLY accessMode correctly results in setting readOnly to true"() {

        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.constraints.*;

@Controller
class PersonController {

    @Operation(
            summary = "Fetch the person information",
            description = "Fetch the person name, debt and goals information",
            parameters = { @Parameter(name = "name", required = true, description = "The person name", in = ParameterIn.PATH) },
            responses = {
                    @ApiResponse(description = "The person information",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema( implementation = Person.class )
                    ))
            }
    )
    @Get("/person/{name}")
    HttpResponse<Person> get(@NotBlank String name) {
        return HttpResponse.ok();
    }
}

/**
 * The person information.
 */
@Introspected
class Person {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Integer id;

    @NotBlank
    private String name;

    @NegativeOrZero
    private Integer debtValue;

    @PositiveOrZero
    private Integer totalGoals;

    public Person(Integer id,
                  @NotBlank String name,
                  @NegativeOrZero Integer debtValue,
                  @PositiveOrZero Integer totalGoals) {

        this.id = id;
        this.name = name;
        this.debtValue = debtValue;
        this.totalGoals = totalGoals;
    }

    /**
     * The person's generated id.
     *
     * @return The id
     */
    public Integer getId() {
        return id;
    }

     /**
     * The person full name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

     /**
     * The total debt amount.
     *
     * @return The debtValue
     */
    public Integer getDebtValue() {
        return debtValue;
    }

     /**
     * The total number of person's goals.
     *
     * @return The totalGoals
     */
    public Integer getTotalGoals() {
        return totalGoals;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDebtValue(Integer debtValue) {
        this.debtValue = debtValue;
    }

    public void setTotalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals;
    }
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        openAPI?.paths?.get("/person/{name}")?.get
        openAPI.components.schemas["Person"]
        openAPI.components.schemas["Person"].type == "object"

        openAPI.components.schemas["Person"].properties
        openAPI.components.schemas["Person"].properties.size() == 4

        openAPI.components.schemas["Person"].properties["id"]
        openAPI.components.schemas["Person"].properties["name"]
        openAPI.components.schemas["Person"].properties["debtValue"]
        openAPI.components.schemas["Person"].properties["totalGoals"]

        openAPI.components.schemas["Person"].properties["id"].type == "integer"
        openAPI.components.schemas["Person"].properties["id"].description == "The person's generated id."
        openAPI.components.schemas["Person"].properties["id"].readOnly
        !openAPI.components.schemas["Person"].properties["id"].writeOnly

        openAPI.components.schemas["Person"].properties["name"].type == "string"
        openAPI.components.schemas["Person"].properties["name"].description == "The person full name."

        openAPI.components.schemas["Person"].properties["debtValue"].type == "integer"
        openAPI.components.schemas["Person"].properties["debtValue"].maximum == 0
        !openAPI.components.schemas["Person"].properties["debtValue"].exclusiveMaximum
        openAPI.components.schemas["Person"].properties["debtValue"].description == "The total debt amount."

        openAPI.components.schemas["Person"].properties["totalGoals"].type == "integer"
        !openAPI.components.schemas["Person"].properties["totalGoals"].exclusiveMinimum
        openAPI.components.schemas["Person"].properties["totalGoals"].description == "The total number of person's goals."
    }

    void "test WRITE_ONLY accessMode correctly results in setting writeOnly to true"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.constraints.*;

@Controller
class PersonController {

    @Operation(
            summary = "Fetch the person information",
            description = "Fetch the person name, debt and goals information",
            parameters = { @Parameter(name = "name", required = true, description = "The person name", in = ParameterIn.PATH) },
            responses = {
                    @ApiResponse(description = "The person information",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema( implementation = Person.class )
                    ))
            }
    )
    @Get("/person/{name}")
    HttpResponse<Person> get(@NotBlank String name) {
        return HttpResponse.ok();
    }
}

/**
 * The person information.
 */
@Introspected
class Person {

    @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
    private Integer id;

    @NotBlank
    private String name;

    @NegativeOrZero
    private Integer debtValue;

    @PositiveOrZero
    private Integer totalGoals;

    Person(Integer id,
                  @NotBlank String name,
                  @NegativeOrZero Integer debtValue,
                  @PositiveOrZero Integer totalGoals) {

        this.id = id;
        this.name = name;
        this.debtValue = debtValue;
        this.totalGoals = totalGoals;
    }

    /**
     * The person's generated id.
     *
     * @return The id
     */
    public Integer getId() {
        return id;
    }

     /**
     * The person full name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

     /**
     * The total debt amount.
     *
     * @return The debtValue
     */
    public Integer getDebtValue() {
        return debtValue;
    }

     /**
     * The total number of person's goals.
     *
     * @return The totalGoals
     */
    public Integer getTotalGoals() {
        return totalGoals;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDebtValue(Integer debtValue) {
        this.debtValue = debtValue;
    }

    public void setTotalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals;
    }
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        openAPI?.paths?.get("/person/{name}")?.get
        openAPI.components.schemas["Person"]
        openAPI.components.schemas["Person"].type == "object"

        openAPI.components.schemas["Person"].properties
        openAPI.components.schemas["Person"].properties.size() == 4

        openAPI.components.schemas["Person"].properties["id"]
        openAPI.components.schemas["Person"].properties["name"]
        openAPI.components.schemas["Person"].properties["debtValue"]
        openAPI.components.schemas["Person"].properties["totalGoals"]

        openAPI.components.schemas["Person"].properties["id"].type == "integer"
        openAPI.components.schemas["Person"].properties["id"].description == "The person's generated id."
        !openAPI.components.schemas["Person"].properties["id"].readOnly
        openAPI.components.schemas["Person"].properties["id"].writeOnly

        openAPI.components.schemas["Person"].properties["name"].type == "string"
        openAPI.components.schemas["Person"].properties["name"].description == "The person full name."

        openAPI.components.schemas["Person"].properties["debtValue"].type == "integer"
        openAPI.components.schemas["Person"].properties["debtValue"].maximum == 0
        !openAPI.components.schemas["Person"].properties["debtValue"].exclusiveMaximum
        openAPI.components.schemas["Person"].properties["debtValue"].description == "The total debt amount."

        openAPI.components.schemas["Person"].properties["totalGoals"].type == "integer"
        !openAPI.components.schemas["Person"].properties["totalGoals"].exclusiveMinimum
        openAPI.components.schemas["Person"].properties["totalGoals"].description == "The total number of person's goals."
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/798")
    void "test Parameter inside Operation"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class PersonController {

    @Get("/{uuid}/users/{userUuid}/services/{serviceCode}")
    @Operation(
        summary = "get user service",
        operationId = "getServiceForUser",
        parameters = {
            @Parameter(
                name = "uuid",
                description = "The identifier",
                schema = @Schema(implementation = String.class),
                in = ParameterIn.PATH
            ),
            @Parameter(
                name = "userUuid",
                description = "The user identifier",
                schema = @Schema(implementation = String.class),
                in = ParameterIn.PATH
            ),
            @Parameter(
                name = "serviceCode",
                description = "The service code",
                schema = @Schema(implementation = String.class),
                in = ParameterIn.PATH
            )
        }
    )
    HttpResponse<Object> get(@NotBlank String uuid, @NotBlank String userUuid, @NotBlank String serviceCode) {
        return HttpResponse.ok();
    }
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        openAPI?.paths?.get("/{uuid}/users/{userUuid}/services/{serviceCode}")?.get
        Operation op = openAPI?.paths?.get("/{uuid}/users/{userUuid}/services/{serviceCode}")?.get
        op.parameters.size() == 3
        op.parameters.get(0).name == 'uuid'
        op.parameters.get(0).schema.type == 'string'
        op.parameters.get(0).in == 'path'
        op.parameters.get(0).description == 'The identifier'
        op.parameters.get(0).example == null
        op.parameters.get(0).$ref == null

        op.parameters.get(1).name == 'userUuid'
        op.parameters.get(1).schema.type == 'string'
        op.parameters.get(1).in == 'path'
        op.parameters.get(1).description == 'The user identifier'
        op.parameters.get(1).example == null
        op.parameters.get(1).$ref == null

        op.parameters.get(2).name == 'serviceCode'
        op.parameters.get(2).schema.type == 'string'
        op.parameters.get(2).in == 'path'
        op.parameters.get(2).description == 'The service code'
        op.parameters.get(2).example == null
        op.parameters.get(2).$ref == null
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/800")
    void "test dto field schema"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class PersonController {

    @Get
    TestDTO get() {
        return new TestDTO();
    }
}

class TestDTO {

    @Schema(defaultValue = "false")
    private Boolean state;

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }
}

@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas.TestDTO
        schema
        schema.properties.state.default == false
        schema.default == null
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/809")
    void "test dto schema with defaultValue"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class TestController {

    @Get("testRest")
    String test(DemoData demoInput) {
        return null;
    }

}

@Introspected
class DemoData {

    @Schema(defaultValue = "myDefault")
    private String name;
    @Schema(defaultValue = "10")
    private Integer propInt;
    @Schema(defaultValue = "100")
    private int propInt2;
    @Schema(defaultValue = "https://example.com")
    private URL url;
    @Schema(defaultValue = "274191c9-c176-4b1c-8263-1b658cbdc7fc")
    private UUID uuid;
    @Schema(defaultValue = "2007-12-03T10:15:30+01:00")
    private Date date;
    @Schema(defaultValue = "myDefault3")
    private MySubObject mySubObject;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPropInt() {
        return propInt;
    }

    public void setPropInt(Integer propInt) {
        this.propInt = propInt;
    }

    public int getPropInt2() {
        return propInt2;
    }

    public void setPropInt2(int propInt2) {
        this.propInt2 = propInt2;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public MySubObject getMySubObject() {
        return mySubObject;
    }

    public void setMySubObject(MySubObject mySubObject) {
        this.mySubObject = mySubObject;
    }
}

@Introspected
class MySubObject {

    private String prop1;

    public String getProp1() {
        return prop1;
    }

    public void setProp1(String prop1) {
        this.prop1 = prop1;
    }
}


@jakarta.inject.Singleton
public class MyBean {}

''')

        then:
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas.DemoData
        schema

        schema.properties.name.default == 'myDefault'
        schema.properties.name.type == 'string'
        schema.properties.name.format == null

        schema.properties.propInt.default == 10
        schema.properties.propInt.type == 'integer'
        schema.properties.propInt.format == 'int32'

        schema.properties.propInt2.default == 100
        schema.properties.propInt2.type == 'integer'
        schema.properties.propInt2.format == 'int32'

        schema.properties.url.default == 'https://example.com'
        schema.properties.url.type == 'string'
        schema.properties.url.format == 'url'

        schema.properties.uuid.default.toString() == '274191c9-c176-4b1c-8263-1b658cbdc7fc'
        schema.properties.uuid.type == 'string'
        schema.properties.uuid.format == 'uuid'

        // TODO: need to add support custom format for DateTime
        schema.properties.date.default == OffsetDateTime.parse('2007-12-03T10:15:30+01:00')
        schema.properties.date.type == 'string'
        schema.properties.date.format == 'date-time'

        schema.properties.mySubObject.allOf.get(1).default == 'myDefault3'
        schema.properties.mySubObject.allOf.get(1).type == null
        schema.properties.mySubObject.allOf.get(1).format == null
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/947")
    void "test dto schema with same name class in different packages"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.openapi.test1.Entity;

@Controller
class TestController {

    @Post("test1")
    String test1(@Body Entity entity) {
        return null;
    }

    @Post("test2")
    String test2(@Body io.micronaut.openapi.test2.Entity entity) {
        return null;
    }

    @Post("test3")
    String test3(@Body io.micronaut.openapi.test3.Entity entity) {
        return null;
    }
}
@jakarta.inject.Singleton
public class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.components.schemas
        openAPI.components.schemas.size() == 3
        Schema entityTest1 = openAPI.components.schemas.Entity
        Schema entityTest2 = openAPI.components.schemas.Entity_1
        Schema entityTest3 = openAPI.components.schemas.Entity_2

        entityTest1
        entityTest1.properties.fieldB
        entityTest1.properties.fieldB.type == 'string'

        entityTest2
        entityTest2.properties.fieldA
        entityTest2.properties.fieldA.type == 'string'

        entityTest3
        entityTest3.properties.fieldC
        entityTest3.properties.fieldC.type == 'string'
    }

    void "test dto schema with same name class in different packages with generics"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.openapi.test1.EntityWithGeneric;

@Controller
class TestController {

    @Post("test1")
    String test1(@Body EntityWithGeneric<String> entity) {
        return null;
    }

    @Post("test11")
    String test11(@Body EntityWithGeneric<Integer> entity) {
        return null;
    }

    @Post("test2")
    String test2(@Body io.micronaut.openapi.test2.EntityWithGeneric<String, Integer> entity) {
        return null;
    }

    @Post("test22")
    String test22(@Body io.micronaut.openapi.test2.EntityWithGeneric<String, String> entity) {
        return null;
    }

    @Post("test3")
    String test3(@Body io.micronaut.openapi.test3.EntityWithGeneric<String, Integer> entity) {
        return null;
    }

    @Post("test33")
    String test33(@Body io.micronaut.openapi.test3.EntityWithGeneric<String, String> entity) {
        return null;
    }
}
@jakarta.inject.Singleton
public class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.components.schemas
        openAPI.components.schemas.size() == 6
        Schema entityTest1 = openAPI.components.schemas.EntityWithGeneric_String_
        Schema entityTest11 = openAPI.components.schemas.EntityWithGeneric_Integer_
        Schema entityTest2 = openAPI.components.schemas."EntityWithGeneric_String.Integer_"
        Schema entityTest22 = openAPI.components.schemas."EntityWithGeneric_String.String_"
        Schema entityTest3 = openAPI.components.schemas."EntityWithGeneric_String.Integer__1"
        Schema entityTest33 = openAPI.components.schemas."EntityWithGeneric_String.String__1"

        entityTest1
        entityTest1.properties.fieldB
        entityTest1.properties.fieldB.type == 'string'

        entityTest11
        entityTest11.properties.fieldB
        entityTest11.properties.fieldB.type == 'integer'

        entityTest2
        entityTest2.properties.fieldA
        entityTest2.properties.fieldA.type == 'string'

        entityTest22
        entityTest22.properties.fieldA
        entityTest22.properties.fieldA.type == 'string'

        entityTest3
        entityTest3.properties.fieldC
        entityTest3.properties.fieldC.type == 'string'

        entityTest33
        entityTest33.properties.fieldC
        entityTest33.properties.fieldC.type == 'string'
    }

    void "test empty default value for Map body type java"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import java.util.Map;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller
class TestController {

    @Post("test1")
    String test1(@Body Map body) {
        return null;
    }
}

@jakarta.inject.Singleton
public class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.paths."/test1".post.requestBody.content."application/json".schema.type == 'object'
        openAPI.paths."/test1".post.requestBody.content."application/json".schema.additionalProperties == true
        openAPI.paths."/test1".post.requestBody.content."application/json".schema.default == null
    }

    void "test oneOf schema"() {
        when:
        buildBeanDefinition("test.MyBean", '''
package test;

import java.util.UUID;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller("/test")
class TestController {

    @Get
    @Operation(summary = "summary", description = "description", responses = @ApiResponse(
          description = "response",
          content = @Content(
                  mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(oneOf = {String.class, UUID.class})
          )
    ))
    void test() {
    }
}

@jakarta.inject.Singleton
public class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        def responseSchema = openAPI.paths."/test".get.responses.'200'.content."application/json".schema

        then:

        responseSchema
        responseSchema.allOf == null
        responseSchema.anyOf == null
        responseSchema.oneOf
        responseSchema.oneOf.size() == 2
    }
}
