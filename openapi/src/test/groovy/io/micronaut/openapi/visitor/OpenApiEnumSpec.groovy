package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema

class OpenApiEnumSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI custom enum jackson mapping"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
class OpenApiController {
    @Get("/currency/{currency}")
    public void postRaw(CurrencyPair currency) {
    }
}

enum CurrencyPair {
    @JsonProperty("CAD-USD")
    CADUSD("CAD", "USD"),
    NOTCADUSD("NOTCAD", "USD"),
    ;

    private final String baseCurrency;
    private final String quoteCurrency;

    CurrencyPair(String baseCurrency, String quoteCurrency) {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['CurrencyPair']
        Operation operation = openAPI.paths."/currency/{currency}".get

        then: "the components are valid"

        schema
        operation
        schema.enum
        schema.enum.size() == 2
        schema.enum.get(0) == "CAD-USD"
        schema.enum.get(1) == "NOTCADUSD"
    }

    void "test build OpenAPI custom enum with different types"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;

@Controller
class OpenApiController {

    @Get("/{myEnum}")
    public void postRaw(MyEnum myEnum) {
    }
}

@Schema(type = "integer", format = "int32", allowableValues = {"1", "2"}, defaultValue = "1")
enum MyEnum {

    VAL1(1),
    VAL2(2),
    ;

    private final int code;

    MyEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @JsonCreator
    public static MyEnum byCode(int code) {
        return Arrays.stream(MyEnum.values())
                .filter(myEnum -> myEnum.code == code)
                .findAny()
                .orElse(null);
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['MyEnum']
        Operation operation = openAPI.paths."/{myEnum}".get

        then: "the components are valid"

        schema
        operation
        schema.default == 1
        schema.enum
        schema.enum.size() == 2
        schema.enum.get(0) == 1
        schema.enum.get(1) == 2
    }

    void "test build OpenAPI custom enum with multiple @JsonProperty"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.Arrays;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

@Controller
class OpenApiController {

    @Get("/{myEnum}")
    public void postRaw(MyEnum myEnum) {
    }
}

@Schema(type = "integer", format = "int32", defaultValue = "1")
enum MyEnum {

    @JsonProperty("1")
    VAL1(1),
    @JsonProperty("2")
    VAL2(2),
    ;

    private final int code;

    MyEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['MyEnum']
        Operation operation = openAPI.paths."/{myEnum}".get

        then: "the components are valid"

        schema
        operation
        schema.default == 1
        schema.enum
        schema.enum.size() == 2
        schema.enum.get(0) == 1
        schema.enum.get(1) == 2
    }

    void "test build OpenAPI enum Schema annotation"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class OpenApiController {

    @Get("/{myEnum}")
    public void postRaw(MyEnum myEnum) {
    }
}

@Schema(implementation = MyEnum.class,
defaultValue = "AWS",
example = "AWS",
description = "Cloud Provider." +
    "\\n- AWS" +
    "<br />Amazon Web Services. " +
    "\\n- AZURE" +
    "<br />Microsoft Azure cloud." +
    "\\n- GCP" +
    "<br />Google Cloud Platform." +
    "\\n- NA" +
    "<br />Not applicable (Self hosting site)."
)
@Validated
@Introspected
enum MyEnum  {

    AWS("Amazon Web Services"),
    AZURE("Microsoft Azure"),
    GCP("Google Cloud Platform"),
    NA("Not applicable");

    private String cloudProvider;

    MyEnum(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['MyEnum']
        Operation operation = openAPI.paths."/{myEnum}".get

        then: "the components are valid"

        schema
        operation
        schema.default == 'AWS'
        schema.example == 'AWS'
        schema.enum
        schema.enum.size() == 4
        schema.enum.get(0) == 'AWS'
        schema.enum.get(1) == 'AZURE'
        schema.enum.get(2) == 'GCP'
        schema.enum.get(3) == 'NA'
        schema.description == "Cloud Provider." +
                "\n- AWS" +
                "<br />Amazon Web Services. " +
                "\n- AZURE" +
                "<br />Microsoft Azure cloud." +
                "\n- GCP" +
                "<br />Google Cloud Platform." +
                "\n- NA" +
                "<br />Not applicable (Self hosting site)."
    }

    void "test build OpenAPI enum Schema annotation with hidden values"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Hidden;import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Controller
class OpenApiController {

    @Get("/{myEnum}")
    public void postRaw(MyEnum myEnum) {
    }
}

@Introspected
enum MyEnum  {

    @Hidden
    AWS("Amazon Web Services"),
    @JsonIgnore
    AZURE("Microsoft Azure"),
    @Schema(hidden = true)
    GCP("Google Cloud Platform"),
    NA("Not applicable");

    private final String cloudProvider;

    MyEnum(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['MyEnum']
        Operation operation = openAPI.paths."/{myEnum}".get

        then: "the components are valid"

        schema
        operation
        schema.enum
        schema.enum.size() == 1
        schema.enum.get(0) == 'NA'
    }

    void "test build OpenAPI enum Schema annotation with allowableValues"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Hidden;import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Controller
class OpenApiController {

    @Get("/{myEnum}")
    public void postRaw(MyEnum myEnum) {
    }
}

@Schema(allowableValues = {"AWS", "AZURE"})
@Introspected
enum MyEnum  {

    AWS("Amazon Web Services"),
    AZURE("Microsoft Azure"),
    GCP("Google Cloud Platform"),
    NA("Not applicable");

    private final String cloudProvider;

    MyEnum(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components.schemas['MyEnum']
        Operation operation = openAPI.paths."/{myEnum}".get

        then: "the components are valid"

        schema
        operation
        schema.enum
        schema.enum.size() == 2
        schema.enum.get(0) == 'AWS'
        schema.enum.get(1) == 'AZURE'
    }
}
