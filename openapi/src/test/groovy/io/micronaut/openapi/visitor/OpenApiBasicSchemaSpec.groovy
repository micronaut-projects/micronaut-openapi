package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI

class OpenApiBasicSchemaSpec extends AbstractTypeElementSpec {

    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

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

@javax.inject.Singleton
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
        openAPI.components.schemas["Person"].properties["debtValue"].description == "The total debt amount."

        openAPI.components.schemas["Person"].properties["totalGoals"].type == "integer"
        openAPI.components.schemas["Person"].properties["totalGoals"].minimum == 0
        openAPI.components.schemas["Person"].properties["totalGoals"].description == "The total number of person's goals."
    }
}
