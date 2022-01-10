package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI

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

import javax.validation.constraints.*;

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
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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

import javax.validation.constraints.*;

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
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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
import javax.validation.constraints.*;
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
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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
        System.setProperty("micronaut.openapi.property.naming.strategy", "SNAKE_CASE")

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

import javax.validation.constraints.*;

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
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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
        System.setProperty("micronaut.openapi.property.naming.strategy", "")
    }

    void "test render OpenApiView specification with LOWER_CAMEL_CASE property naming strategy - Issue #241"() {
        given:
        System.setProperty("micronaut.openapi.property.naming.strategy", "LOWER_CAMEL_CASE")

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

import javax.validation.constraints.*;

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
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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
        System.setProperty("micronaut.openapi.property.naming.strategy", "")
    }

    void "test render OpenApiView specification with custom property naming strategy and required properties - Issue #240"() {
        given:
        System.setProperty("micronaut.openapi.property.naming.strategy", "SNAKE_CASE")

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

import javax.validation.constraints.*;

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
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        openAPI?.paths?.get("/person/{name}")?.get
        openAPI.components.schemas["Person"]
        openAPI.components.schemas["Person"].type == "object"

        openAPI.components.schemas["Person"].properties
        openAPI.components.schemas["Person"].properties.size() == 4

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

        openAPI.components.schemas["Person"].properties["xyz"].type == "string"
        openAPI.components.schemas["Person"].properties["xyz"].additionalProperties == null
        openAPI.components.schemas["Person"].properties["xyz"].format == "email"

        openAPI.components.schemas["Person"].required.size() == 3
        openAPI.components.schemas["Person"].required.contains("name")
        openAPI.components.schemas["Person"].required.contains("debt_value")
        openAPI.components.schemas["Person"].required.contains("total_goals")

        cleanup:
        System.setProperty("micronaut.openapi.property.naming.strategy", "")
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

import javax.validation.constraints.*;

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
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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

import javax.validation.constraints.*;

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
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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

}
