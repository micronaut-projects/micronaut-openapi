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
        Operation operation = openAPI.paths.get("/path").post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema instanceof Schema
        ((Schema) operation.requestBody.content."application/json".schema).get$ref() == "#/components/schemas/MyDto"

        dtoSchema.properties.test instanceof ComposedSchema
        dtoSchema.properties.test.allOf
        dtoSchema.properties.test.allOf.size() == 2
        dtoSchema.properties.test.allOf.get(0).$ref == "#/components/schemas/Parameters"
        dtoSchema.properties.test.allOf.get(1).description == 'this is description'
        dtoSchema.properties.test.allOf.get(1).default
        dtoSchema.properties.test.allOf.get(1).default.stampWidth == 100
        dtoSchema.properties.test.allOf.get(1).example
        dtoSchema.properties.test.allOf.get(1).example.stampWidth == 220
        dtoSchema.properties.test.allOf.get(1).example.stampHeight == 85
        dtoSchema.properties.test.allOf.get(1).example.pageNumber == 1
        dtoSchema.properties.test.allOf.get(1).deprecated
        dtoSchema.properties.test.allOf.get(1).readOnly
        dtoSchema.properties.test.allOf.get(1).format == 'binary'
        dtoSchema.properties.test.allOf.get(1).externalDocs.description == 'external docs'
        dtoSchema.properties.test.allOf.get(1).title == 'the title'
        dtoSchema.properties.test.allOf.get(1).exclusiveMinimum
        dtoSchema.properties.test.allOf.get(1).exclusiveMaximum
        dtoSchema.properties.test.allOf.get(1).maximum == 100
        dtoSchema.properties.test.allOf.get(1).minimum == 10
        dtoSchema.properties.test.allOf.get(1).maximum == 100
        dtoSchema.properties.test.allOf.get(1).minLength == 10
        dtoSchema.properties.test.allOf.get(1).maxLength == 100
        dtoSchema.properties.test.allOf.get(1).minProperties == 10
        dtoSchema.properties.test.allOf.get(1).maxProperties == 100
        dtoSchema.properties.test.allOf.get(1).multipleOf == 1.5
        dtoSchema.properties.test.allOf.get(1).pattern == "ppp"
        dtoSchema.properties.test.allOf.get(1).additionalProperties == true
        dtoSchema.properties.test.nullable
        dtoSchema.required.size() == 1
        dtoSchema.required.get(0) == 'test'
    }

    void "test schema example in class schema"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.List;
import java.time.Instant;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
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
        name = "ParametersShema",
        description = "this is description",
        nullable = true,
        deprecated = true,
        maxLength = 10,
        minLength = 1,
        minimum = "5",
        maximum = "20",
        accessMode = Schema.AccessMode.READ_ONLY,
        defaultValue = "{\\"stampWidth\\": 100}",
        required = true,
        example = "{\\n" +
                "  \\"stampWidth\\": 220,\\n" +
                "  \\"stampHeight\\": 85,\\n" +
                "  \\"pageNumber\\": 1\\n" +
                "}"
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
        Schema parametersSchema = openAPI.components.schemas['ParametersShema']

        then: "the components are valid"
        dtoSchema != null
        parametersSchema != null

        dtoSchema instanceof Schema
        parametersSchema instanceof Schema

        when:
        Operation operation = openAPI.paths.get("/path").post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema instanceof Schema
        ((Schema) operation.requestBody.content."application/json".schema).get$ref() == "#/components/schemas/MyDto"

        dtoSchema.properties.parameters.get$ref() == "#/components/schemas/ParametersShema"
        !openAPI.components.schemas.MyDto.required
        openAPI.components.schemas.ParametersShema.deprecated
        openAPI.components.schemas.ParametersShema.nullable
        openAPI.components.schemas.ParametersShema.readOnly
        openAPI.components.schemas.ParametersShema.description == 'this is description'
        openAPI.components.schemas.ParametersShema.example
        openAPI.components.schemas.ParametersShema.example.stampWidth == 220
        openAPI.components.schemas.ParametersShema.example.stampHeight == 85
        openAPI.components.schemas.ParametersShema.example.pageNumber == 1
        openAPI.components.schemas.ParametersShema.default
        openAPI.components.schemas.ParametersShema.default.stampWidth == 100
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
        Operation operation = openAPI.paths.get("/path").post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema instanceof Schema
        ((Schema) operation.requestBody.content."application/json".schema).get$ref() == "#/components/schemas/MyDto"

        dtoSchema.properties.test instanceof ComposedSchema
        dtoSchema.properties.test.allOf
        dtoSchema.properties.test.allOf.size() == 2
        dtoSchema.properties.test.allOf.get(0).$ref == "#/components/schemas/Parameters"
        dtoSchema.properties.test.allOf.get(1).description == 'this is description'
        !dtoSchema.properties.test.allOf.get(1).default
        !dtoSchema.properties.test.allOf.get(1).example
        !dtoSchema.properties.test.allOf.get(1).deprecated
        !dtoSchema.properties.test.allOf.get(1).readOnly
        !dtoSchema.properties.test.allOf.get(1).format
        !dtoSchema.properties.test.allOf.get(1).externalDocs
        !dtoSchema.properties.test.allOf.get(1).title
        !dtoSchema.properties.test.allOf.get(1).exclusiveMinimum
        !dtoSchema.properties.test.allOf.get(1).exclusiveMaximum
        !dtoSchema.properties.test.allOf.get(1).maximum
        !dtoSchema.properties.test.allOf.get(1).minimum
        !dtoSchema.properties.test.allOf.get(1).maximum
        !dtoSchema.properties.test.allOf.get(1).minLength
        !dtoSchema.properties.test.allOf.get(1).maxLength
        !dtoSchema.properties.test.allOf.get(1).minProperties
        !dtoSchema.properties.test.allOf.get(1).maxProperties
        !dtoSchema.properties.test.allOf.get(1).multipleOf
        !dtoSchema.properties.test.allOf.get(1).pattern
        !dtoSchema.properties.test.allOf.get(1).additionalProperties
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
        Operation operation = openAPI.paths.get("/path").post

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
        dtoSchema.properties.test.allOf.get(0).$ref == '#/components/schemas/GlobalParams'
        dtoSchema.properties.test.allOf.get(1).not
        dtoSchema.properties.test.allOf.get(1).not.$ref == '#/components/schemas/LocalParams'
        dtoSchema.properties.test.allOf.get(1).allOf.get(0).$ref == '#/components/schemas/LocalParams'
        dtoSchema.properties.test.allOf.get(1).oneOf.get(0).$ref == '#/components/schemas/LocalParams'
        dtoSchema.properties.test.allOf.get(1).anyOf.get(0).$ref == '#/components/schemas/LocalParams'

        globalParamsSchema.properties.globalStampWidth.type == 'integer'
        globalParamsSchema.properties.globalStampWidth.format == 'int32'

        localParamsSchema.allOf.size() == 2
        localParamsSchema.allOf.get(0).$ref == '#/components/schemas/GlobalParams'
        localParamsSchema.allOf.get(1).properties.stampWidth.type == 'integer'
        localParamsSchema.allOf.get(1).properties.stampWidth.format == 'int32'
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
        Operation operation = openAPI.paths.get("/path").post

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
        localParamsSchema.allOf.get(0).$ref == '#/components/schemas/GlobalParams'
        localParamsSchema.allOf.get(1).properties.stampWidth.type == 'integer'
        localParamsSchema.allOf.get(1).properties.stampWidth.format == 'int32'

        dtoSchema.properties.test instanceof ComposedSchema
        dtoSchema.properties.test.allOf.get(0).$ref == '#/components/schemas/LocalParams'
        dtoSchema.properties.test.allOf.get(1).description == 'this is description'
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
        Operation operation = openAPI.paths.get("/path").post

        then:
        operation

        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema instanceof Schema
        ((Schema) operation.requestBody.content."application/json".schema).$ref == "#/components/schemas/test"

        dtoSchema.not
        dtoSchema.not.$ref == '#/components/schemas/LocalParams'
        dtoSchema.allOf.get(0).$ref == '#/components/schemas/LocalParams'
        dtoSchema.allOf.get(1).properties.parameters.$ref == '#/components/schemas/GlobalParams'
        dtoSchema.allOf.get(1).description == 'this is description'
        dtoSchema.oneOf.get(0).$ref == '#/components/schemas/LocalParams'
        dtoSchema.anyOf.get(0).$ref == '#/components/schemas/LocalParams'

        globalParamsSchema.properties.globalStampWidth.type == 'integer'
        globalParamsSchema.properties.globalStampWidth.format == 'int32'

        localParamsSchema.allOf.size() == 2
        localParamsSchema.allOf.get(0).$ref == '#/components/schemas/GlobalParams'
        localParamsSchema.allOf.get(1).properties.stampWidth.type == 'integer'
        localParamsSchema.allOf.get(1).properties.stampWidth.format == 'int32'
    }
}
