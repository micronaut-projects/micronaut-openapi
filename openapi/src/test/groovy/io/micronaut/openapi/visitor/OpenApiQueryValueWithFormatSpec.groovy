package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.parameters.Parameter

class OpenApiQueryValueWithFormatSpec extends AbstractOpenApiTypeElementSpec {

    void "test query value with format (Iterable)"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.convert.format.Format;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import java.util.List;

import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_CSV;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_DEEP_OBJECT;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_MULTI;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_PIPES;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_SSV;

@Controller("/list")
interface ListOps {

    @Get("/default")
    void opDefault(@QueryValue List<String> param);

    @Get("/csv")
    void opSimple(@QueryValue @Format(FORMAT_CSV) List<String> param);

    @Get("/space")
    void opSpace(@QueryValue @Format(FORMAT_SSV) List<String> param);

    @Get("/pipes")
    void opPipes(@QueryValue @Format(FORMAT_PIPES) List<String> param);

    @Get("/deepObject")
    void opDeepObject(@QueryValue @Format(FORMAT_DEEP_OBJECT) List<String> param);

    @Get("/multi")
    void opMulti(@QueryValue @Format(FORMAT_MULTI) List<String> param);
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        var paths = openAPI.paths

        then:
        !paths."/list/default".get.parameters[0].style
        paths."/list/default".get.parameters[0].explode == false
        paths."/list/default".get.parameters[0].schema.type == "array"
        !paths."/list/csv".get.parameters[0].style
        paths."/list/csv".get.parameters[0].explode == false
        paths."/list/csv".get.parameters[0].schema.type == "array"
        paths."/list/space".get.parameters[0].style == Parameter.StyleEnum.SPACEDELIMITED
        paths."/list/space".get.parameters[0].schema.type == "array"
        paths."/list/pipes".get.parameters[0].style == Parameter.StyleEnum.PIPEDELIMITED
        paths."/list/pipes".get.parameters[0].schema.type == "array"
        paths."/list/deepObject".get.parameters[0].style == Parameter.StyleEnum.DEEPOBJECT
        paths."/list/deepObject".get.parameters[0].schema.type == "array"
        !paths."/list/multi".get.parameters[0].style
        paths."/list/multi".get.parameters[0].schema.type == "array"
    }

    void "test query value with format (Map)"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.convert.format.Format;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import java.util.Map;

import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_CSV;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_DEEP_OBJECT;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_MULTI;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_PIPES;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_SSV;

@Controller("/map")
interface MapOps {

    @Get("/default")
    void opDefault(@QueryValue Map<String, Integer> param);

    @Get("/csv")
    void opSimple(@QueryValue @Format(FORMAT_CSV) Map<String, Integer> param);

    @Get("/space")
    void opSpace(@QueryValue @Format(FORMAT_SSV) Map<String, Integer> param);

    @Get("/pipes")
    void opPipes(@QueryValue @Format(FORMAT_PIPES) Map<String, Integer> param);

    @Get("/deepObject")
    void opDeepObject(@QueryValue @Format(FORMAT_DEEP_OBJECT) Map<String, Object> param);

    @Get("/multi")
    void opMulti(@QueryValue @Format(FORMAT_MULTI) Map<String, Object> param);
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        var paths = openAPI.paths

        then:
        !paths."/map/default".get.parameters[0].style
        paths."/map/default".get.parameters[0].explode == false
        paths."/map/default".get.parameters[0].schema.type == "object"
        paths."/map/default".get.parameters[0].schema.additionalProperties
        paths."/map/default".get.parameters[0].schema.additionalProperties.type == "integer"
        !paths."/map/csv".get.parameters[0].style
        paths."/map/csv".get.parameters[0].explode == false
        paths."/map/csv".get.parameters[0].schema.type == "object"
        paths."/map/csv".get.parameters[0].schema.additionalProperties
        paths."/map/csv".get.parameters[0].schema.additionalProperties.type == "integer"
        paths."/map/space".get.parameters[0].style == Parameter.StyleEnum.SPACEDELIMITED
        paths."/map/space".get.parameters[0].schema.type == "object"
        paths."/map/space".get.parameters[0].schema.additionalProperties
        paths."/map/space".get.parameters[0].schema.additionalProperties.type == "integer"
        paths."/map/pipes".get.parameters[0].style == Parameter.StyleEnum.PIPEDELIMITED
        paths."/map/pipes".get.parameters[0].schema.type == "object"
        paths."/map/pipes".get.parameters[0].schema.additionalProperties
        paths."/map/pipes".get.parameters[0].schema.additionalProperties.type == "integer"
        paths."/map/deepObject".get.parameters[0].style == Parameter.StyleEnum.DEEPOBJECT
        paths."/map/deepObject".get.parameters[0].schema.type == "object"
        paths."/map/deepObject".get.parameters[0].schema.additionalProperties
        paths."/map/deepObject".get.parameters[0].schema.additionalProperties.type == "object"
        !paths."/map/multi".get.parameters[0].style
        paths."/map/multi".get.parameters[0].schema.type == "object"
        paths."/map/multi".get.parameters[0].schema.additionalProperties
        paths."/map/multi".get.parameters[0].schema.additionalProperties.type == "object"
    }

    void "test query value with format (Object)"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.convert.format.Format;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_CSV;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_DEEP_OBJECT;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_MULTI;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_PIPES;
import static io.micronaut.core.convert.converters.MultiValuesConverterFactory.FORMAT_SSV;

@Controller("/object")
interface ObjectOps {

    @Get("/default")
    void opDefault(@QueryValue Object param);

    @Get("/csv")
    void opSimple(@QueryValue @Format(FORMAT_CSV) Object param);

    @Get("/space")
    void opSpace(@QueryValue @Format(FORMAT_SSV) Object param);

    @Get("/pipes")
    void opPipes(@QueryValue @Format(FORMAT_PIPES) Object param);

    @Get("/deepObject")
    void opDeepObject(@QueryValue @Format(FORMAT_DEEP_OBJECT) Object param);

    @Get("/multi")
    void opMulti(@QueryValue @Format(FORMAT_MULTI) Object param);
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        var paths = openAPI.paths

        then:
        !paths."/object/default".get.parameters[0].style
        paths."/object/default".get.parameters[0].explode == false
        paths."/object/default".get.parameters[0].schema
        !paths."/object/csv".get.parameters[0].style
        paths."/object/csv".get.parameters[0].explode == false
        paths."/object/csv".get.parameters[0].schema
        paths."/object/space".get.parameters[0].style == Parameter.StyleEnum.SPACEDELIMITED
        paths."/object/space".get.parameters[0].schema
        paths."/object/pipes".get.parameters[0].style == Parameter.StyleEnum.PIPEDELIMITED
        paths."/object/pipes".get.parameters[0].schema
        paths."/object/deepObject".get.parameters[0].style == Parameter.StyleEnum.DEEPOBJECT
        paths."/object/deepObject".get.parameters[0].schema
        !paths."/object/multi".get.parameters[0].style
        paths."/object/multi".get.parameters[0].schema
    }
}
