package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import spock.util.environment.RestoreSystemProperties

import static io.micronaut.openapi.visitor.GeneratorExt.DEPRECATED_MESSAGE
import static io.micronaut.openapi.visitor.GeneratorExt.ENUM_DEPRECATED
import static io.micronaut.openapi.visitor.GeneratorExt.ENUM_DESCRIPTIONS
import static io.micronaut.openapi.visitor.GeneratorExt.ENUM_VAR_NAMES
import static io.micronaut.openapi.visitor.GeneratorExt.MAX_MESSAGE
import static io.micronaut.openapi.visitor.GeneratorExt.MIN_MESSAGE
import static io.micronaut.openapi.visitor.GeneratorExt.NOT_NULL_MESSAGE
import static io.micronaut.openapi.visitor.GeneratorExt.PATTERN_MESSAGE
import static io.micronaut.openapi.visitor.GeneratorExt.SIZE_MESSAGE
import static io.micronaut.openapi.visitor.GeneratorExt.TYPE

class OpenApiGeneratorExtensionsSpec extends AbstractOpenApiTypeElementSpec {

    @RestoreSystemProperties
    void "test deprecated messages extensions"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_GENERATOR_EXTENSIONS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

/**
 * @deprecated This is deprecated controller
 */
@Deprecated
@Controller
class DeprecatedApi {

    @Get("/deprecated-op1")
    public MyObj deprecatedOp1() {
        return null;
    }

    @Get("/deprecated-op2")
    public MyObj deprecatedOp2() {
        return null;
    }
}

@Controller
class HelloWorldApi {

    /**
     * This is description.
     * 
     * @param name this is deprecated parameter.
     * 
     * @deprecated This is deprecated operation
     */
    @Deprecated
    @Get("/get/user/{id}")
    public MyObj helloWorld(
        String id,
        @Deprecated @QueryValue String name) {
        return null;
    }
}

/**
 * Old schema class.
 * 
 * @deprecated This class is deprecated.
 */
@Deprecated
class MyObj {
    
    /**
     * It's deprecated property.
     * 
     * @deprecated need to use another property.
     */
    @Deprecated
    public String oldProp;
    public String newProp;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference

        then:
        openApi.paths."/deprecated-op1".get.deprecated
        openApi.paths."/deprecated-op1".get.extensions[DEPRECATED_MESSAGE] == "This is deprecated controller"
        openApi.paths."/deprecated-op2".get.deprecated
        openApi.paths."/deprecated-op2".get.extensions[DEPRECATED_MESSAGE] == "This is deprecated controller"

        openApi.paths."/get/user/{id}".get.deprecated
        openApi.paths."/get/user/{id}".get.extensions[DEPRECATED_MESSAGE] == "This is deprecated operation"

        openApi.paths."/get/user/{id}".get.deprecated
        openApi.paths."/get/user/{id}".get.extensions[DEPRECATED_MESSAGE] == "This is deprecated operation"
        openApi.paths."/get/user/{id}".get.parameters[1].name == 'name'
        openApi.paths."/get/user/{id}".get.parameters[1].deprecated

        openApi.components.schemas.MyObj.deprecated
        openApi.components.schemas.MyObj.extensions[DEPRECATED_MESSAGE] == "This class is deprecated."

        openApi.components.schemas.MyObj.properties.oldProp.deprecated
        openApi.components.schemas.MyObj.properties.oldProp.extensions[DEPRECATED_MESSAGE] == "need to use another property."
    }

    @RestoreSystemProperties
    void "test enum extensions"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_GENERATOR_EXTENSIONS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class HelloWorldApi {

    @Get("/get/user/{id}")
    public MyEnum helloWorld(String id) {
        return null;
    }
}

/**
 * @deprecated deprecated enum.
 */
@Deprecated
enum MyEnum {
    
    @JsonProperty("12")
    ENUM_VAR1((byte) 12),
    /**
     * Enum const description.
     */
    @JsonProperty("13")
    ENUM_VAR2((byte) 13),
    /**
     * @deprecated Enum const deprecated.
     */
    @Deprecated
    @JsonProperty("14")
    ENUM_VAR3((byte) 14),
    @Schema(name = "CUSTOM_ENUM_VAR_NAME")
    @JsonProperty("15")
    ENUM_VAR4((byte) 15),
    @Deprecated
    @Schema(name = "CUSTOM_ENUM_VAR_NAME2")
    @JsonProperty("15")
    ENUM_VAR5((byte) 16),
    ;
    
    private final byte value;
    
    MyEnum(byte value) {
        this.value = value;
    }
    
    @JsonValue
    public byte getValue() {
        return value;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference
        def schema = openApi.components.schemas.MyEnum

        then:

        schema
        schema.deprecated
        schema.extensions[DEPRECATED_MESSAGE] == "deprecated enum."
        schema.extensions[TYPE] == "byte"

        def descriptions = (List<String>) schema.extensions[ENUM_DESCRIPTIONS]
        descriptions[0] == ""
        descriptions[1] == "Enum const description."
        descriptions[2] == ""
        descriptions[3] == ""

        def enumVarNames = (List<String>) schema.extensions[ENUM_VAR_NAMES]
        enumVarNames[0] == "ENUM_VAR1"
        enumVarNames[1] == "ENUM_VAR2"
        enumVarNames[2] == "ENUM_VAR3"
        enumVarNames[3] == "CUSTOM_ENUM_VAR_NAME"

        def deprecatedList = (List<String>) schema.extensions[ENUM_DEPRECATED]
        deprecatedList.size() == 2
        deprecatedList[0] == "ENUM_VAR3"
        deprecatedList[1] == "CUSTOM_ENUM_VAR_NAME2"
    }

    @RestoreSystemProperties
    void "test validation messages extensions"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_GENERATOR_EXTENSIONS_ENABLED, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

@Controller
class HelloWorldApi {

    @Get("/get/user/{id}")
    public MyObj helloWorld(String id) {
        return null;
    }
}

class MyObj {
    
    @Size(min = 10, message = "size collection message")
    public List<@Pattern(regexp = "rrr", message = "pattern mes") @NotNull(message = "not null mes") String> sizeCollectionProp;
    @NotEmpty(message = "not empty collection message")
    public List<String> notEmptyCollectionProp;
    @NotNull(message = "not null message")
    public String notNullProp;
    @NotEmpty(message = "not empty message")
    public String notEmptyProp;
    @NotBlank(message = "not blank message")
    public String notBlankProp;
    @Size(min = 10, message = "size string message")
    public String sizeStringProp;
    @Min(value = 10, message = "min int message")
    public int minIntProp;
    @Max(value = 10, message = "max int message")
    public int maxIntProp;
    @DecimalMin(value = "10", message = "min decimal message")
    public double minDecimalProp;
    @DecimalMax(value = "10", message = "max decimal message")
    public double maxDecimalProp;
    @Negative(message = "negative message")
    public int negativeProp;
    @NegativeOrZero(message = "negative or zero message")
    public int negativeOrZeroProp;
    @Positive(message = "positive message")
    public int positiveProp;
    @PositiveOrZero(message = "positive or zero message")
    public int positiveOrZeroProp;
    @Pattern(regexp = "reg", message = "pattern message")
    public String patternProp;
    @Email(regexp = "reg", message = "email message")
    public String emailProp;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference != null

        when:
        def openApi = Utils.testReference
        def schema = openApi.components.schemas.MyObj

        then:

        schema
        schema.properties.sizeCollectionProp.extensions[SIZE_MESSAGE] == "size collection message"
        schema.properties.sizeCollectionProp.items.extensions[PATTERN_MESSAGE] == "pattern mes"
        schema.properties.sizeCollectionProp.items.extensions[NOT_NULL_MESSAGE] == "not null mes"
        schema.properties.notEmptyCollectionProp.extensions[SIZE_MESSAGE] == "not empty collection message"
        schema.properties.notNullProp.extensions[NOT_NULL_MESSAGE] == "not null message"
        schema.properties.notEmptyProp.extensions[SIZE_MESSAGE] == "not empty message"
        schema.properties.notBlankProp.extensions[SIZE_MESSAGE] == "not blank message"
        schema.properties.sizeStringProp.extensions[SIZE_MESSAGE] == "size string message"
        schema.properties.minIntProp.extensions[MIN_MESSAGE] == "min int message"
        schema.properties.maxIntProp.extensions[MAX_MESSAGE] == "max int message"
        schema.properties.minDecimalProp.extensions[MIN_MESSAGE] == "min decimal message"
        schema.properties.maxDecimalProp.extensions[MAX_MESSAGE] == "max decimal message"
        schema.properties.negativeProp.extensions[MAX_MESSAGE] == "negative message"
        schema.properties.negativeOrZeroProp.extensions[MAX_MESSAGE] == "negative or zero message"
        schema.properties.positiveProp.extensions[MIN_MESSAGE] == "positive message"
        schema.properties.positiveOrZeroProp.extensions[MIN_MESSAGE] == "positive or zero message"
        schema.properties.patternProp.extensions[PATTERN_MESSAGE] == "pattern message"
        schema.properties.emailProp.extensions[PATTERN_MESSAGE] == "email message"
    }
}
