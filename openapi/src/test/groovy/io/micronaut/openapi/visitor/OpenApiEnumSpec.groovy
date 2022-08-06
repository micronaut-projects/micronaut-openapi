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

    void "test build OpenAPI custom enum jwith different types"() {

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
}
