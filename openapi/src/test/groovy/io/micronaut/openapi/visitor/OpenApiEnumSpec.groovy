package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.micronaut.openapi.swagger.PrimitiveType
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema

class OpenApiEnumSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI custom enum jackson JsonValue method schema type"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@Controller
class OpenApiController {
    @Get("/currency/{currency}")
    public void postRaw(MyEnum myEnum) {
    }
}

enum MyEnum {

    @JsonProperty("1")
    FIRST(1),
    @JsonProperty("2")
    SECOND(2),
    ;

    private final int value;

    MyEnum(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
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

        then: "the components are valid"

        openAPI.components.schemas.size() == 1
        schema
        schema.type == PrimitiveType.INT.commonName
        schema.enum
        schema.enum.size() == 2
        schema.enum.get(0) == 1
        schema.enum.get(1) == 2
    }

    void "test build OpenAPI custom enum with custom schema type"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

@Controller
class OpenApiController {
    @Get("/currency/{currency}")
    public void postRaw(MyEnum myEnum) {
    }
}

@Schema(type = "integer")
enum MyEnum {

    @JsonProperty("1")
    FIRST(1),
    @JsonProperty("2")
    SECOND(2),
    ;

    private final int value;

    MyEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
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

        then: "the components are valid"

        openAPI.components.schemas.size() == 1

        schema
        schema.type == PrimitiveType.INT.commonName
        schema.enum
        schema.enum.size() == 2
        schema.enum.get(0) == 1
        schema.enum.get(1) == 2
    }

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

    void "test build OpenAPI enum Schema with byte type values"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.List;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@Controller
class OpenApiController {

    @Post
    public void postRaw(DictionaryRequest request) {
    }
}

class DictionaryRequest {

    protected Type1 type1;
    protected Type2 type2;

    public Type1 getType1() {
        return type1;
    }

    public void setType1(Type1 type1) {
        this.type1 = type1;
    }

    public Type2 getType2() {
        return type2;
    }

    public void setType2(Type2 type2) {
        this.type2 = type2;
    }
}

enum Type1 {

    @JsonProperty("1")
    _1(((byte) 1)),
    @JsonProperty("2")
    _2(((byte) 2));

    private final byte value;

    Type1(byte value) {
        this.value = value;
    }

    @JsonValue
    public byte value() {
        return value;
    }

    @JsonCreator
    public static Type1 fromValue(byte v) {
        for (Type1 c : values()) {
            if (c.value == v) {
                return c;
            }
        }
        throw new IllegalArgumentException(String.valueOf(v));
    }
}

enum Type2 {

    @JsonProperty("111")
    _1("111"),
    @JsonProperty("222")
    _2("222");

    private final byte[] value;

    Type2(String value) {
        this.value = value.getBytes();
    }

    @JsonValue
    public byte[] value() {
        return value;
    }

    @JsonCreator
    public static Type2 fromValue(byte[] v) {
        for (Type2 c : values()) {
            if (c.value == v) {
                return c;
            }
        }
        throw new IllegalArgumentException(String.valueOf(v));
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReferenceAfterPlaceholders != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReferenceAfterPlaceholders

        then: "the state is correct"
        openAPI.components
        openAPI.components.schemas
        openAPI.components.schemas.size() == 3

        when:
        Schema requestSchema = openAPI.components.schemas['DictionaryRequest']
        Schema type1Schema = openAPI.components.schemas['Type1']
        Schema type2Schema = openAPI.components.schemas['Type2']

        then: "the components are valid"
        requestSchema
        type1Schema
        type1Schema.enum
        type1Schema.enum.size() == 2
        type1Schema.type == 'integer'
        type1Schema.format == 'int32'
        type1Schema.enum.contains(1)
        type1Schema.enum.contains(2)

        type2Schema
        type2Schema.enum
        type2Schema.enum.size() == 2
        type2Schema.type == 'string'
        type2Schema.format == 'byte'
        type2Schema.enum.contains("111")
        type2Schema.enum.contains("222")
    }

    void "test build OpenAPI enum Schema with JsonValue returned another enum"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.List;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@Controller
class OpenApiController {

    @Post
    public void postRaw(DictionaryRequest request) {
    }
}

class DictionaryRequest {

    protected Type1 type1;

    public Type1 getType1() {
        return type1;
    }

    public void setType1(Type1 type1) {
        this.type1 = type1;
    }
}

enum Type1 {

    @JsonProperty("558301010000")
    _558301010000(Type2._558301010000),
    @JsonProperty("558301020000")
    _558301020000(Type2._558301020000);

    private final Type2 value;

    Type1(Type2 value) {
        this.value = value;
    }

    @JsonValue
    public Type2 value() {
        return value;
    }

    @JsonCreator
    public static Type1 fromValue(Type2 v) {
        for (Type1 c : values()) {
            if (c.value == v) {
                return c;
            }
        }
        throw new IllegalArgumentException(String.valueOf(v));
    }
}

enum Type2 {

    @JsonProperty("558301010000")
    _558301010000("558301010000"),
    @JsonProperty("558301020000")
    _558301020000("558301020000");

    private final String value;

    Type2(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static Type2 fromValue(String v) {
        for (Type2 c : values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(String.valueOf(v));
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReferenceAfterPlaceholders != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReferenceAfterPlaceholders

        then: "the state is correct"
        openAPI.components
        openAPI.components.schemas
        openAPI.components.schemas.size() == 2

        when:
        Schema requestSchema = openAPI.components.schemas['DictionaryRequest']
        Schema type1Schema = openAPI.components.schemas['Type1']

        then: "the components are valid"
        requestSchema
        type1Schema
        type1Schema.enum
        type1Schema.enum.size() == 2
        type1Schema.type == 'string'
        type1Schema.enum.contains("558301010000")
        type1Schema.enum.contains("558301020000")
    }

    void "test enum as schema property"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class OpenApiController {

    @Post
    public ReadBackupSettingsExDto getBackupSettings11() {
        return null;
    }
}

@Introspected
@Schema(description = "Schema that represents the current backup settings")
class ReadBackupSettingsExDto {

    @Schema(description = "Required backup frequency", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
    private BackupFrequencyExDto requiredBackupFrequency;

    @Schema(description = "Optional backup frequency. The key/value be omitted in the JSON result.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private BackupFrequencyExDto optionalBackupFrequency;

    public BackupFrequencyExDto getRequiredBackupFrequency() {
        return requiredBackupFrequency;
    }

    public void setRequiredBackupFrequency(BackupFrequencyExDto requiredBackupFrequency) {
        this.requiredBackupFrequency = requiredBackupFrequency;
    }

    public BackupFrequencyExDto getOptionalBackupFrequency() {
        return optionalBackupFrequency;
    }

    public void setOptionalBackupFrequency(BackupFrequencyExDto optionalBackupFrequency) {
        this.optionalBackupFrequency = optionalBackupFrequency;
    }
}

@Introspected
@Schema(description = "Schema that represents the frequency of the backup")
enum BackupFrequencyExDto {

    DAILY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReferenceAfterPlaceholders != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReferenceAfterPlaceholders

        then: "the state is correct"
        openAPI.components
        openAPI.components.schemas
        openAPI.components.schemas.size() == 2

        when:
        Schema backupFrequencySchema = openAPI.components.schemas.BackupFrequencyExDto
        Schema readBackupSchema = openAPI.components.schemas.ReadBackupSettingsExDto

        then: "the components are valid"
        backupFrequencySchema
        readBackupSchema
        backupFrequencySchema.enum
        backupFrequencySchema.enum.size() == 8
        backupFrequencySchema.type == 'string'
    }
}
