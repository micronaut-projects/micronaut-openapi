package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.micronaut.openapi.swagger.core.util.PrimitiveType
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import spock.lang.Unroll

class OpenApiEmptySchemaSpec extends AbstractOpenApiTypeElementSpec {

    def setup() {
        PrimitiveType.explicitObjectType = true
    }

    def cleanup() {
        PrimitiveType.explicitObjectType = false
    }

    void "test described reference properties don't leak a redundant object schema into allOf"() {
        when:
        buildBeanDefinition('test.MyBean', '''

package test;

import java.util.List;
import java.util.Map;
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

    @Schema(description = "the parameters description")
    private Parameters parameters;

    @Schema(description = "the color description")
    private Color color;

    @Schema(description = "the list description")
    private List<Parameters> parametersList;

    @Schema(description = "the map description")
    private Map<String, Parameters> parametersMap;

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public List<Parameters> getParametersList() {
        return parametersList;
    }

    public void setParametersList(List<Parameters> parametersList) {
        this.parametersList = parametersList;
    }

    public Map<String, Parameters> getParametersMap() {
        return parametersMap;
    }

    public void setParametersMap(Map<String, Parameters> parametersMap) {
        this.parametersMap = parametersMap;
    }
}

class Parameters {

    private Integer stampWidth;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }
}

enum Color {
    RED,
    GREEN,
    BLUE
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openApi = Utils.testReference
        var dtoSchema = openApi.components.schemas['MyDto']

        then: "the described POJO property keeps only the \$ref, without a redundant object schema"
        dtoSchema != null
        dtoSchema.properties.parameters.description == 'the parameters description'
        dtoSchema.properties.parameters.allOf
        dtoSchema.properties.parameters.allOf.size() == 1
        dtoSchema.properties.parameters.allOf[0].$ref == "#/components/schemas/Parameters"

        and: "the described enum property keeps only the \$ref, without a redundant object schema"
        dtoSchema.properties.color.description == 'the color description'
        dtoSchema.properties.color.allOf
        dtoSchema.properties.color.allOf.size() == 1
        dtoSchema.properties.color.allOf[0].$ref == "#/components/schemas/Color"

        and: "the described list property references items directly, without an allOf or a redundant object schema"
        dtoSchema.properties.parametersList.description == 'the list description'
        dtoSchema.properties.parametersList.type == 'array'
        dtoSchema.properties.parametersList.allOf == null
        dtoSchema.properties.parametersList.items.$ref == "#/components/schemas/Parameters"

        and: "the described map property references additionalProperties directly, without an allOf or a redundant object schema"
        dtoSchema.properties.parametersMap.description == 'the map description'
        dtoSchema.properties.parametersMap.allOf == null
        dtoSchema.properties.parametersMap.additionalProperties.$ref == "#/components/schemas/Parameters"
    }

    @Unroll
    void "test isEmptySchema treats a functionally-empty schema (#caseName) as empty"() {
        expect:
        SchemaUtils.isEmptySchema(schema)

        where:
        caseName                            | schema
        'plain empty schema'                | new Schema()
        'object schema'                     | new ObjectSchema()
        'object schema with null type'      | new ObjectSchema().type(null)
        'generic schema of type object'     | new Schema().type(SchemaUtils.TYPE_OBJECT)
        'map schema'                        | new MapSchema()
        'string schema'                     | new StringSchema()
        'string schema with null type'      | new StringSchema().type(null)
        'generic schema of type string'     | new Schema().type(SchemaUtils.TYPE_STRING)
        'integer schema'                    | new IntegerSchema()
        'integer schema with null type'     | new IntegerSchema().type(null)
        'generic schema of type integer'    | new Schema().type(SchemaUtils.TYPE_INTEGER)
        'number schema'                     | new NumberSchema()
        'generic schema of type number'     | new Schema().type(SchemaUtils.TYPE_NUMBER)
        'boolean schema'                    | new BooleanSchema()
        'generic schema of type boolean'    | new Schema().type(SchemaUtils.TYPE_BOOLEAN)
        'array schema'                      | new ArraySchema()
        'array schema with null type'       | new ArraySchema().type(null)
        'generic schema of type array'      | new Schema().type(SchemaUtils.TYPE_ARRAY)
    }

    @Unroll
    void "test isEmptySchema does not treat a schema carrying content (#caseName) as empty"() {
        expect:
        !SchemaUtils.isEmptySchema(schema)

        where:
        caseName                          | schema
        'null schema'                     | null
        'object schema with description'  | new Schema().type(SchemaUtils.TYPE_OBJECT).description('desc')
        'string schema with description'  | new Schema().type(SchemaUtils.TYPE_STRING).description('desc')
        'schema with a \$ref'             | new Schema().$ref('#/components/schemas/Foo')
        'object schema with a property'   | new ObjectSchema().properties(['field': new StringSchema()])
        'array schema with items'         | new ArraySchema().items(new StringSchema())
        'string schema with a format'     | new Schema().type(SchemaUtils.TYPE_STRING).format('uuid')
    }
}
