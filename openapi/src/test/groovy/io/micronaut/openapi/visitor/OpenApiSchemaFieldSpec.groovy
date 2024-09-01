package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema

class OpenApiSchemaFieldSpec extends AbstractOpenApiTypeElementSpec {

    void "test schema example in parameter schema"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.List;
import java.time.Instant;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

class MyDto {

    @Schema(
            name = "test",
            description = "this is description",
            nullable = true,
            deprecated = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            defaultValue = "{\\"stampWidth\\": 100}",
            required = true,
            format = "binary",
            title = "the title",
            minimum = "10",
            maximum = "100",
            exclusiveMinimum = true,
            exclusiveMaximum = true,
            minLength = 10,
            maxLength = 100,
            minProperties = 10,
            maxProperties = 100,
            multipleOf = 1.5,
            pattern = "ppp",
            additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
            externalDocs = @ExternalDocumentation(description = "external docs"),
            example = "{\\n" +
                    "  \\"stampWidth\\": 220,\\n" +
                    "  \\"stampHeight\\": 85,\\n" +
                    "  \\"pageNumber\\": 1\\n" +
                    "}"
    )
    private Parameters parameters;

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}

class Parameters {

    private Integer stampWidth;
    private Integer stampHeight;
    private int pageNumber;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }

    public Integer getStampHeight() {
        return stampHeight;
    }

    public void setStampHeight(Integer stampHeight) {
        this.stampHeight = stampHeight;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema dtoSchema = openAPI.components.schemas['MyDto']
        Schema parametersSchema = openAPI.components.schemas['Parameters']

        then: "the components are valid"
        dtoSchema != null
        parametersSchema != null

        dtoSchema instanceof Schema
        parametersSchema instanceof Schema

        when:
        Operation operation = openAPI.paths."/path".post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema instanceof Schema
        ((Schema) operation.requestBody.content."application/json".schema).get$ref() == "#/components/schemas/MyDto"

        dtoSchema.properties.test.title == 'the title'
        dtoSchema.properties.test.deprecated
        dtoSchema.properties.test.readOnly
        dtoSchema.properties.test.description == 'this is description'
        dtoSchema.properties.test.externalDocs.description == 'external docs'
        dtoSchema.properties.test.example
        dtoSchema.properties.test.example.stampWidth == 220
        dtoSchema.properties.test.example.stampHeight == 85
        dtoSchema.properties.test.example.pageNumber == 1
        dtoSchema.properties.test.default
        dtoSchema.properties.test.default.stampWidth == 100
        dtoSchema.properties.test.allOf
        dtoSchema.properties.test.allOf.size() == 2
        dtoSchema.properties.test.allOf[0].$ref == "#/components/schemas/Parameters"
        dtoSchema.properties.test.allOf[1].format == 'binary'
        dtoSchema.properties.test.allOf[1].exclusiveMinimum
        dtoSchema.properties.test.allOf[1].exclusiveMaximum
        dtoSchema.properties.test.allOf[1].maximum == 100
        dtoSchema.properties.test.allOf[1].minimum == 10
        dtoSchema.properties.test.allOf[1].maximum == 100
        dtoSchema.properties.test.allOf[1].minLength == 10
        dtoSchema.properties.test.allOf[1].maxLength == 100
        dtoSchema.properties.test.allOf[1].minProperties == 10
        dtoSchema.properties.test.allOf[1].maxProperties == 100
        dtoSchema.properties.test.allOf[1].multipleOf == 1.5
        dtoSchema.properties.test.allOf[1].pattern == "ppp"
        dtoSchema.properties.test.allOf[1].additionalProperties == true
        dtoSchema.properties.test.nullable
        dtoSchema.required.size() == 1
        dtoSchema.required[0] == 'test'
    }

    void "test schema example in class schema"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

class MyDto {

    private Parameters parameters;

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}

@Schema(
        name = "ParametersSchema",
        description = "this is description",
        nullable = true,
        deprecated = true,
        maxLength = 10,
        minLength = 1,
        minimum = "5",
        maximum = "20",
        accessMode = Schema.AccessMode.READ_ONLY,
        defaultValue = """
                {"stampWidth": 100}""",
        required = true,
        example = """
                {
                    "stampWidth": 220,
                    "stampHeight": 85,
                    "pageNumber": 1
                }""",
        extensions = {
                @Extension(name = "ext1", properties = @ExtensionProperty(name = "prop11", value = "val11")),
                @Extension(name = "ext2", properties = @ExtensionProperty(name = "prop21", value = "val21")),
        }
)
class Parameters {

    private Integer stampWidth;
    private Integer stampHeight;
    private int pageNumber;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }

    public Integer getStampHeight() {
        return stampHeight;
    }

    public void setStampHeight(Integer stampHeight) {
        this.stampHeight = stampHeight;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema dtoSchema = openAPI.components.schemas['MyDto']
        Schema parametersSchema = openAPI.components.schemas['ParametersSchema']

        then: "the components are valid"
        dtoSchema
        parametersSchema

        when:
        Operation operation = openAPI.paths."/path".post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema instanceof Schema
        ((Schema) operation.requestBody.content."application/json".schema).get$ref() == "#/components/schemas/MyDto"

        dtoSchema.properties.parameters.get$ref() == "#/components/schemas/ParametersSchema"
        !openAPI.components.schemas.MyDto.required

        def schema = openAPI.components.schemas.ParametersSchema

        schema.deprecated
        schema.nullable
        schema.readOnly
        schema.description == 'this is description'
        schema.example
        schema.example.stampWidth == 220
        schema.example.stampHeight == 85
        schema.example.pageNumber == 1
        schema.default
        schema.default.stampWidth == 100

        schema.extensions.'x-ext1'
        schema.extensions.'x-ext1'.prop11 == 'val11'
        schema.extensions.'x-ext2'
        schema.extensions.'x-ext2'.prop21 == 'val21'
    }

    void "test schema on property level with default values"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.List;
import java.time.Instant;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

class MyDto {

    @Schema(
            name = "test",
            description = "this is description"
    )
    private Parameters parameters;

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}

class Parameters {

    private Integer stampWidth;
    private Integer stampHeight;
    private int pageNumber;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }

    public Integer getStampHeight() {
        return stampHeight;
    }

    public void setStampHeight(Integer stampHeight) {
        this.stampHeight = stampHeight;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema dtoSchema = openAPI.components.schemas['MyDto']
        Schema parametersSchema = openAPI.components.schemas['Parameters']

        then: "the components are valid"
        dtoSchema != null
        parametersSchema != null

        dtoSchema instanceof Schema
        parametersSchema instanceof Schema

        when:
        Operation operation = openAPI.paths."/path".post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema instanceof Schema
        ((Schema) operation.requestBody.content."application/json".schema).get$ref() == "#/components/schemas/MyDto"

        dtoSchema.properties.test.description == 'this is description'
        dtoSchema.properties.test.allOf
        dtoSchema.properties.test.allOf.size() == 1
        dtoSchema.properties.test.allOf[0].$ref == "#/components/schemas/Parameters"
        !dtoSchema.properties.test.default
        !dtoSchema.properties.test.example
        !dtoSchema.properties.test.deprecated
        !dtoSchema.properties.test.readOnly
        !dtoSchema.properties.test.format
        !dtoSchema.properties.test.externalDocs
        !dtoSchema.properties.test.title
        !dtoSchema.properties.test.exclusiveMinimum
        !dtoSchema.properties.test.exclusiveMaximum
        !dtoSchema.properties.test.maximum
        !dtoSchema.properties.test.minimum
        !dtoSchema.properties.test.maximum
        !dtoSchema.properties.test.minLength
        !dtoSchema.properties.test.maxLength
        !dtoSchema.properties.test.minProperties
        !dtoSchema.properties.test.maxProperties
        !dtoSchema.properties.test.multipleOf
        !dtoSchema.properties.test.pattern
        !dtoSchema.properties.test.additionalProperties
        !dtoSchema.properties.test.nullable
        !dtoSchema.required
    }

    void "test schema on property level with not/allOf/anyOf/oneOf"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.List;
import java.time.Instant;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

class MyDto {

    @Schema(
            name = "test",
            description = "this is description",
            not = LocalParams.class,
            anyOf = LocalParams.class,
            oneOf = LocalParams.class,
            allOf = LocalParams.class
    )
    private GlobalParams parameters;

    public GlobalParams getParameters() {
        return parameters;
    }

    public void setParameters(GlobalParams parameters) {
        this.parameters = parameters;
    }
}

class Parameters extends GlobalParams {

    private Integer stampWidth;
    private Integer stampHeight;
    private int pageNumber;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }

    public Integer getStampHeight() {
        return stampHeight;
    }

    public void setStampHeight(Integer stampHeight) {
        this.stampHeight = stampHeight;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}

class LocalParams extends GlobalParams {

    private Integer stampWidth;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }
}

class GlobalParams {

    private Integer globalStampWidth;

    public Integer getGlobalStampWidth() {
        return globalStampWidth;
    }

    public void setGlobalStampWidth(Integer globalStampWidth) {
        this.globalStampWidth = globalStampWidth;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema dtoSchema = openAPI.components.schemas.MyDto
        Schema localParamsSchema = openAPI.components.schemas.LocalParams
        Schema globalParamsSchema = openAPI.components.schemas.GlobalParams

        then: "the components are valid"
        dtoSchema
        localParamsSchema
        globalParamsSchema

        when:
        Operation operation = openAPI.paths."/path".post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema instanceof Schema
        ((Schema) operation.requestBody.content."application/json".schema).$ref == "#/components/schemas/MyDto"

        dtoSchema.properties.test instanceof ComposedSchema
        dtoSchema.properties.test.allOf[0].$ref == '#/components/schemas/GlobalParams'
        dtoSchema.properties.test.allOf[1].allOf[0].$ref == '#/components/schemas/LocalParams'
        dtoSchema.properties.test.not
        dtoSchema.properties.test.not.$ref == '#/components/schemas/LocalParams'
        dtoSchema.properties.test.oneOf[0].$ref == '#/components/schemas/LocalParams'
        dtoSchema.properties.test.anyOf[0].$ref == '#/components/schemas/LocalParams'

        globalParamsSchema.properties.globalStampWidth.type == 'integer'
        globalParamsSchema.properties.globalStampWidth.format == 'int32'

        localParamsSchema.allOf.size() == 2
        localParamsSchema.allOf[0].$ref == '#/components/schemas/GlobalParams'
        localParamsSchema.allOf[1].properties.stampWidth.type == 'integer'
        localParamsSchema.allOf[1].properties.stampWidth.format == 'int32'
    }

    void "test schema on property level with implementation"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.List;
import java.time.Instant;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

class MyDto {

    @Schema(
            name = "test",
            description = "this is description",
            implementation = LocalParams.class
    )
    private GlobalParams parameters;

    public GlobalParams getParameters() {
        return parameters;
    }

    public void setParameters(GlobalParams parameters) {
        this.parameters = parameters;
    }
}

class Parameters extends GlobalParams {

    private Integer stampWidth;
    private Integer stampHeight;
    private int pageNumber;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }

    public Integer getStampHeight() {
        return stampHeight;
    }

    public void setStampHeight(Integer stampHeight) {
        this.stampHeight = stampHeight;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}

class LocalParams extends GlobalParams {

    private Integer stampWidth;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }
}

class GlobalParams {

    private Integer globalStampWidth;

    public Integer getGlobalStampWidth() {
        return globalStampWidth;
    }

    public void setGlobalStampWidth(Integer globalStampWidth) {
        this.globalStampWidth = globalStampWidth;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema dtoSchema = openAPI.components.schemas.MyDto
        Schema localParamsSchema = openAPI.components.schemas.LocalParams
        Schema globalParamsSchema = openAPI.components.schemas.GlobalParams

        then: "the components are valid"
        dtoSchema
        localParamsSchema
        globalParamsSchema

        when:
        Operation operation = openAPI.paths."/path".post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema instanceof Schema
        ((Schema) operation.requestBody.content."application/json".schema).$ref == "#/components/schemas/MyDto"

        localParamsSchema.allOf.size() == 2
        localParamsSchema.allOf[0].$ref == '#/components/schemas/GlobalParams'
        localParamsSchema.allOf[1].properties.stampWidth.type == 'integer'
        localParamsSchema.allOf[1].properties.stampWidth.format == 'int32'

        dtoSchema.properties.test.description == 'this is description'
        dtoSchema.properties.test.allOf.size() == 1
        dtoSchema.properties.test.allOf[0].$ref == '#/components/schemas/LocalParams'
    }

    void "test schema on class level with not/allOf/anyOf/oneOf"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.List;
import java.time.Instant;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

@Schema(
        name = "test",
        description = "this is description",
        not = LocalParams.class,
        anyOf = LocalParams.class,
        oneOf = LocalParams.class,
        allOf = LocalParams.class
)
class MyDto {

    private GlobalParams parameters;

    public GlobalParams getParameters() {
        return parameters;
    }

    public void setParameters(GlobalParams parameters) {
        this.parameters = parameters;
    }
}

class Parameters extends GlobalParams {

    private Integer stampWidth;
    private Integer stampHeight;
    private int pageNumber;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }

    public Integer getStampHeight() {
        return stampHeight;
    }

    public void setStampHeight(Integer stampHeight) {
        this.stampHeight = stampHeight;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}

class LocalParams extends GlobalParams {

    private Integer stampWidth;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }
}

class GlobalParams {

    private Integer globalStampWidth;

    public Integer getGlobalStampWidth() {
        return globalStampWidth;
    }

    public void setGlobalStampWidth(Integer globalStampWidth) {
        this.globalStampWidth = globalStampWidth;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema dtoSchema = openAPI.components.schemas.test
        Schema localParamsSchema = openAPI.components.schemas.LocalParams
        Schema globalParamsSchema = openAPI.components.schemas.GlobalParams

        then: "the components are valid"
        dtoSchema
        localParamsSchema
        globalParamsSchema

        when:
        Operation operation = openAPI.paths."/path".post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema instanceof Schema
        ((Schema) operation.requestBody.content."application/json".schema).$ref == "#/components/schemas/test"

        dtoSchema.description == 'this is description'
        dtoSchema.not
        dtoSchema.not.$ref == '#/components/schemas/LocalParams'
        dtoSchema.allOf[0].$ref == '#/components/schemas/LocalParams'
        dtoSchema.allOf[1].properties.parameters.$ref == '#/components/schemas/GlobalParams'
        dtoSchema.oneOf[0].$ref == '#/components/schemas/LocalParams'
        dtoSchema.anyOf[0].$ref == '#/components/schemas/LocalParams'

        globalParamsSchema.properties.globalStampWidth.type == 'integer'
        globalParamsSchema.properties.globalStampWidth.format == 'int32'

        localParamsSchema.allOf.size() == 2
        localParamsSchema.allOf[0].$ref == '#/components/schemas/GlobalParams'
        localParamsSchema.allOf[1].properties.stampWidth.type == 'integer'
        localParamsSchema.allOf[1].properties.stampWidth.format == 'int32'
    }

    void "test schema on property level with type"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.time.ZoneId;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Schema;

@OpenAPIDefinition(
        info = @Info(
                title = "Hello World",
                version = "42",
                description = "This is it. The answer to life , the universe and everything",
                license = @License(name = "Apache 2.0", url = "https://foo.bar"),
                contact = @Contact(url = "https://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
        )
)
@Controller("/exemplars")
class StampSyncController {
    @Post
    Exemplar create(@Body Exemplar toBeCreated) {
        return new Exemplar(ZoneId.of("America/New_York"));
    }
}

@Schema(name = "exemplar")
class Exemplar {
    @Schema(name = "zone_id", type = "string")
    private ZoneId zoneId;

    Exemplar(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        then:
        openAPI.components.schemas
        openAPI.components.schemas.size() == 1

        when:
        Schema dtoSchema = openAPI.components.schemas.exemplar

        then: "the components are valid"
        dtoSchema
        dtoSchema.properties.zone_id.type == 'string'
    }

    void "test annotations on constructor parameters level"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.math.BigDecimal;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Controller
class HelloController {

    @Put("/sendModelWithDiscriminator")
    Mono<Animal> sendModelWithDiscriminator(
        @Body @NotNull @Valid Animal animal
    ) {
        return Mono.empty();
    }
}

@Serdeable
@JsonIgnoreProperties(
        value = "class", // ignore manually set class, it will be automatically generated by Jackson during serialization
        allowSetters = true // allows the class to be set during deserialization
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "class", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Bird.class, name = "ave"),
})
class Animal {

    @JsonProperty("class")
    protected String propertyClass;
    @Schema(name = "color", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Nullable
    private ColorEnum color;

    public String getPropertyClass() {
        return propertyClass;
    }

    public void setPropertyClass(String propertyClass) {
        this.propertyClass = propertyClass;
    }

    public ColorEnum getColor() {
        return color;
    }

    public void setColor(ColorEnum color) {
        this.color = color;
    }
}

@Serdeable
class Bird extends Animal {

    private Integer numWings;
    @DecimalMax("123.78")
    private BigDecimal beakLength;
    private String featherDescription;

    Bird(
        @Min(10) Integer numWings,
        @JsonProperty("myLength") BigDecimal beakLength,
        String featherDescription) {

    }

    public Integer getNumWings() {
        return numWings;
    }

    public void setNumWings(Integer numWings) {
        this.numWings = numWings;
    }

    public BigDecimal getBeakLength() {
        return beakLength;
    }

    public void setBeakLength(BigDecimal beakLength) {
        this.beakLength = beakLength;
    }

    public String getFeatherDescription() {
        return featherDescription;
    }

    public void setFeatherDescription(String featherDescription) {
        this.featherDescription = featherDescription;
    }
}

@Serdeable
enum ColorEnum {

    @JsonProperty("red")
    RED
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference
        def schemas = openApi.components.schemas

        then: "the components are valid"
        schemas.Animal
        schemas.Bird
        schemas.ColorEnum

        !schemas.Bird.allOf[1].properties.beakLength
        schemas.Bird.allOf[1].properties.myLength
        schemas.Bird.allOf[1].properties.myLength.maximum == 123.78
        schemas.Bird.allOf[1].properties.numWings.minimum == 10
    }

    void "test unwrap allOf"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class HelloController {

    @Put("/sendModelWithDiscriminator")
    MyDto2 sendModelWithDiscriminator() {
        return null;
    }
}

@Schema(description = "A simple DTO")
class MyDto {
  @Schema(description = "A string field")
  public String field1;
}

@Schema(description = "A DTO containing the other DTO")
class MyDto2 {
  @Schema(
      description = "A field containing another DTO",
      title = "my title",
      deprecated = true,
      nullable = true
  )
  public MyDto field2;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference
        def schemas = openApi.components.schemas

        then: "the components are valid"
        schemas.MyDto2
        schemas.MyDto2.properties.field2.title == "my title"
        schemas.MyDto2.properties.field2.description == "A field containing another DTO"
        schemas.MyDto2.properties.field2.deprecated
        schemas.MyDto2.properties.field2.nullable
        schemas.MyDto2.properties.field2.allOf
        schemas.MyDto2.properties.field2.allOf.size() == 1
        schemas.MyDto2.properties.field2.allOf[0].$ref == '#/components/schemas/MyDto'
    }

    void "test unwrap with custom schema implementation"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class HelloController {

    @Put("/sendModelWithDiscriminator")
    Car sendModelWithDiscriminator() {
        return null;
    }
}

record Car(
    String model, 
    @JsonUnwrapped @Schema(implementation = ResourcePath.class) Resource resource,
    boolean selected
) { }

abstract class Resource {
}

abstract class ResourcePath extends Resource {

  public String resourcePath;
  public String resourceType;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference
        def schemas = openApi.components.schemas

        then: "the components are valid"
        schemas.Car
        schemas.Car.properties.model
        schemas.Car.properties.resourcePath
        schemas.Car.properties.resourceType
        schemas.Car.properties.selected
    }

    void "test schema additionalProperties = false"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Nullable;import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.io.Serializable;
import java.util.List;

@Controller
class HelloController {

    @Post(value = "/search", consumes = "application/json", produces = "application/json")
    @Operation(summary = "Returns List of Audit Log records based on the matching search criteria", description = "Returns list of Audit Log records matching the search critera. SLA:500", responses = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of list of Audit Log records matching the search criteria", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = AuditLogDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid input provided", content = { @Content(mediaType = "application/json")}), 
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = { @Content(mediaType = "application/json")}),})            
    public List<AuditLogDTO> searchAuditLog(
            @Parameter(description = "Page number for the search results", example = "1", required = false) @QueryValue(value = "page", defaultValue = "1") @Nullable Integer page,
            @Parameter(description = "Size of the page", example = "1", required = false) @QueryValue(value = "size", defaultValue = "100") @Nullable Integer size,
            @Parameter(description = "Audit Search criteria", example = "1", required = true) @Body @Valid AuditSearchCriteria auditSearchCriteria) {
        return null;
    }

}

enum TestEnum {
    VAL1, VAL2
}

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
class AuditSearchCriteria implements Serializable {
        
}

record AuditLogDTO(
    TestEnum field1,
    @io.swagger.v3.oas.annotations.media.Schema(deprecated = true) TestEnum field2
) {
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference
        def schemas = openApi.components.schemas

        then: "the components are valid"
        schemas.AuditSearchCriteria.additionalProperties != null
        schemas.AuditSearchCriteria.additionalProperties == false
    }

    void "test jackson and swagger annotations together"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonClassDescription;import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Controller
class HelloController {

    @Get
    ResourceSearchVO sendModelWithDiscriminator() {
        return null;
    }
}

@JsonClassDescription("Jackson schema description")
class ResourceSearchVO implements Serializable {

    /**
     * Must be swagger description (javadoc)
     */
    @Schema(description = "Must be swagger description (swagger)")
    @JsonPropertyDescription("Must be swagger description (jackson)")
    public String swaggerDesc;
    /**
     * Must be jackson description (javadoc)
     */
    @JsonPropertyDescription("Must be jackson description (jackson)")
    public String jacksonDesc;
    /**
     * Must be javadoc description (javadoc)
     */
    public String javadocDesc;
    
    @Schema(defaultValue = "VAL1")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY, defaultValue = "VAL2")
    public ResourceEnum type;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY, defaultValue = "VAL2")
    public ResourceEnum type1;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, defaultValue = "VAL2")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY, defaultValue = "VAL1")
    public ResourceEnum type2;
    public ResourceEnum type3;
}

enum ResourceEnum {
    VAL1,
    VAL2
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference
        def schemas = openApi.components.schemas

        then: "the components are valid"
        schemas.ResourceSearchVO
        schemas.ResourceSearchVO.description == 'Jackson schema description'
        schemas.ResourceSearchVO.properties.swaggerDesc.description == "Must be swagger description (swagger)"
        schemas.ResourceSearchVO.properties.jacksonDesc.description == 'Must be jackson description (jackson)'
        schemas.ResourceSearchVO.properties.javadocDesc.description == 'Must be javadoc description (javadoc)'
        schemas.ResourceSearchVO.properties.type.allOf[0].$ref == '#/components/schemas/ResourceEnum'
        schemas.ResourceSearchVO.properties.type.writeOnly == true
        schemas.ResourceSearchVO.properties.type.default == "VAL1"
        schemas.ResourceSearchVO.properties.type1.allOf[0].$ref == '#/components/schemas/ResourceEnum'
        schemas.ResourceSearchVO.properties.type1.writeOnly == true
        schemas.ResourceSearchVO.properties.type1.default == "VAL2"
        schemas.ResourceSearchVO.properties.type2.allOf[0].$ref == '#/components/schemas/ResourceEnum'
        schemas.ResourceSearchVO.properties.type2.readOnly == true
        !schemas.ResourceSearchVO.properties.type2.writeOnly
        schemas.ResourceSearchVO.properties.type2.default == "VAL2"
        schemas.ResourceSearchVO.properties.type3.$ref == '#/components/schemas/ResourceEnum'
    }

    void "test JsonFormat on enum"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

@Controller
class HelloController {

    @Get("/foo")
    User getFoo(@QueryValue @Nullable User user) {
        return null;
    }
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum User {

    FOO(1, "Foo"),
    BAR(2, "Bar");

    @JsonProperty("id")
    public final int id;

    @JsonProperty("name")
    public final String name;

    User(int id, String name) {
        this.id = id;
        this.name = name;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference
        def schemas = openApi.components.schemas

        then: "the components are valid"
        schemas.User
        schemas.User.type == 'object'
        schemas.User.required
        schemas.User.required.size() == 2
        schemas.User.required[0] == 'id'
        schemas.User.required[1] == 'name'
        schemas.User.properties.id.type == "integer"
        schemas.User.properties.id.format == "int32"
        schemas.User.properties.name.type == "string"
    }
}
