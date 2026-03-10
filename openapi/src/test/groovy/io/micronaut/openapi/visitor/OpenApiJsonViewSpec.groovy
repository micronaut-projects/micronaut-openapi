package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema

class OpenApiJsonViewSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI with JsonView"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_JACKSON_JSON_VIEW_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.List;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import com.fasterxml.jackson.annotation.JsonView;

@Controller
class OpenApiController {

    @Get("/summary")
    @JsonView(View.Summary.class)
    @Operation(summary = "Return car summaries",
            responses = @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Car.class)))))
    public HttpResponse<?> getSummaries() {
        return null;
    }

    @Get("/detail")
    @JsonView(View.Detail.class)
    @ApiResponse(responseCode = "200", description = "Return car detail", content = @Content(schema = @Schema(implementation = Car.class)))
    @Operation(summary = "Return car detail")
    public List<Car> getDetails() {
        return null;
    }

    /**
     * {@summary Return car sale summary}
     */
    @Get("/sale")
    @JsonView(View.Sale.class)
    public List<Car> getSaleSummaries() {
        return null;
    }

    @Post("/add")
    public void addCar(@JsonView(View.Sale.class) @Body Car car) {
    }
}

interface View {

    interface Summary {}

    interface Detail extends Summary {}

    interface Sale {}
}

class Car {

    @JsonView(View.Summary.class)
    private String made;

    @JsonView({View.Summary.class, View.Detail.class})
    private String model;

    @JsonView(View.Detail.class)
    private List<Tire> tires;

    @JsonView(View.Sale.class)
    private int price;

    @JsonView({View.Sale.class, View.Summary.class})
    private int age;

    // common
    private String color;

    public String getColor() {
        return color;
    }

    public String getMade() {
        return made;
    }

    public void setMade(String made) {
        this.made = made;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Tire> getTires() {
        return tires;
    }

    public void setTires(List<Tire> tires) {
        this.tires = tires;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setColor(String color) {
        this.color = color;
    }
}

class Tire {

    @JsonView(View.Summary.class)
    private String made;

    @JsonView(View.Detail.class)
    private String condition;

    public String getMade() {
        return made;
    }

    public void setMade(String made) {
        this.made = made;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema carDetail = openAPI.components.schemas['Car_Detail']
        Schema carSale = openAPI.components.schemas['Car_Sale']
        Schema carSummary = openAPI.components.schemas['Car_Summary']
        Schema tireDetail = openAPI.components.schemas['Tire_Detail']
        Operation addOp = openAPI.paths."/add".post
        Operation detailOp = openAPI.paths."/detail".get
        Operation saleOp = openAPI.paths."/sale".get
        Operation summaryOp = openAPI.paths."/summary".get

        then:

        addOp
        addOp.requestBody.content.'application/json'.schema.$ref == '#/components/schemas/Car_Sale'

        detailOp
        detailOp.responses.'200'.content.'application/json'.schema.$ref == '#/components/schemas/Car_Detail'

        saleOp
        saleOp.responses.'200'.content.'application/json'.schema.items.$ref == '#/components/schemas/Car_Sale'

        summaryOp
        summaryOp.responses.'200'.content.'application/json'.schema.items.$ref == '#/components/schemas/Car_Summary'

        carDetail
        carDetail.properties.size() == 5
        carDetail.properties.color
        carDetail.properties.made
        carDetail.properties.model
        carDetail.properties.tires
        carDetail.properties.age

        carSale
        carSale.properties.size() == 3
        carSale.properties.color
        carSale.properties.price
        carSale.properties.age

        carSummary
        carSummary.properties.size() == 4
        carSummary.properties.color
        carSummary.properties.made
        carSummary.properties.model
        carSummary.properties.age

        tireDetail
        tireDetail.properties.size() == 2
        tireDetail.properties.made
        tireDetail.properties.condition

        cleanup:
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_JACKSON_JSON_VIEW_ENABLED)
    }

    void "test build OpenAPI with changed JsonView default inclusion"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_JACKSON_JSON_VIEW_ENABLED, "true")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_JSON_VIEW_DEFAULT_INCLUSION, "false")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.List;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import com.fasterxml.jackson.annotation.JsonView;

@Controller
class OpenApiController {

    @Get("/summary")
    @JsonView(View.Summary.class)
    @Operation(summary = "Return car summaries",
            responses = @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Car.class)))))
    public HttpResponse<?> getSummaries() {
        return null;
    }
}

interface View {

    interface Summary {}
}

class Car {

    @JsonView(View.Summary.class)
    public String made;
    public String model;
    public List<Tire> tires;
    public int price;
    public int age;
    public String color;
}

class Tire {

    public String made;
    public String condition;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema carSummary = openAPI.components.schemas['Car_Summary']
        Operation summaryOp = openAPI.paths."/summary".get

        then:

        summaryOp
        summaryOp.responses.'200'.content.'application/json'.schema.items.$ref == '#/components/schemas/Car_Summary'

        carSummary
        carSummary.properties.size() == 1
        carSummary.properties.made

        cleanup:
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_JACKSON_JSON_VIEW_ENABLED)
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_JSON_VIEW_DEFAULT_INCLUSION)
    }
}
