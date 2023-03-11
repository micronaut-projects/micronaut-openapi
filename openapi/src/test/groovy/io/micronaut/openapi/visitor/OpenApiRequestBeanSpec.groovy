package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiRequestBeanSpec extends AbstractOpenApiTypeElementSpec {

    void "test basic @RequestBean annotation"() {
        given:
            buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import io.micronaut.core.annotation.Nullable;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import java.util.List;

@Controller("/")
class MyController {

    @Get("/{pV}")
    public Response updatePet(@RequestBean MyRequestBean bean) {
        return null;
    }
}

@Introspected
class MyRequestBean {

    public static final String HEADER_CONTENT_TYPE = "My-Content-type";

    HttpRequest<?> httpRequest;

    @PathVariable("pV")
    @Parameter(description="Any path variable")
    private String pathVariable;

    @QueryValue("qV")
    @Parameter(description="Any query value")
    private String queryValue;

    @Nullable
    @Parameter(description="Any content type")
    @Header(HEADER_CONTENT_TYPE)
    private String contentType;

    MyRequestBean(HttpRequest<?> httpRequest, String pathVariable, String queryValue, String contentType) {
        this.httpRequest = httpRequest;
        this.pathVariable = pathVariable;
        this.queryValue = queryValue;
        this.contentType = contentType;
    }

    public HttpRequest<?> getHttpRequest() {
        return httpRequest;
    }

    public String getPathVariable() {
        return pathVariable;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public String getContentType() {
        return contentType;
    }

}

class Response {}

@jakarta.inject.Singleton
class MyBean {}

''')

            OpenAPI openAPI = Utils.testReference
            Operation operation = openAPI.paths?.get("/{pV}")?.get

        expect:
            operation
            operation.parameters
            operation.parameters.size() == 3
            operation.parameters[0].name == 'pV'
            operation.parameters[0].description == 'Any path variable'
            operation.parameters[0].in == 'path'
            operation.parameters[0].required
            operation.parameters[1].name == 'qV'
            operation.parameters[1].description == 'Any query value'
            operation.parameters[1].in == 'query'
            operation.parameters[1].required
            operation.parameters[2].name == 'My-Content-type'
            operation.parameters[2].description == 'Any content type'
            operation.parameters[2].in == 'header'
            !operation.parameters[2].required
    }

    void "test @RequestBean annotation duplicate props"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import io.micronaut.core.annotation.Nullable;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.links.*;
import java.util.List;

@Controller
class SimpleController {

    @Get("{path}/simple")
    public String getPath(@RequestBean PathWrapper wrapper) {
        return wrapper.getPath();
    }
}

class BaseController {
}

@Controller("/inherits")
class SimpleControllerInherits extends BaseController  {

    @Get("{path}/simple")
    public String getPath(@RequestBean PathWrapper wrapper) {
        return wrapper.getPath();
    }
}

@Introspected
class PathWrapper {

    @PathVariable
    private final String path;

    PathWrapper(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}

@jakarta.inject.Singleton
class MyBean {}

''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/{path}/simple")?.get
        Operation operationInherit = openAPI.paths?.get("/inherits/{path}/simple")?.get

        expect:
        operation
        operation.parameters
        operation.parameters.size() == 1
        operation.parameters[0].name == 'path'
        operation.parameters[0].in == 'path'
        operation.parameters[0].required

        operationInherit
        operationInherit.parameters
        operationInherit.parameters.size() == 1
        operationInherit.parameters[0].name == 'path'
        operationInherit.parameters[0].in == 'path'
        operationInherit.parameters[0].required
    }

}
