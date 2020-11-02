package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiRequestBeanSpec extends AbstractTypeElementSpec {

    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    void "test basic @RequestBean annotation"() {
        given:
            buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import edu.umd.cs.findbugs.annotations.Nullable;
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

    HttpRequest<?> httpRequest;
   
    @PathVariable("pV")
    @Parameter(description="Any path variable")
    private String pathVariable;
    
    @QueryValue("qV")
    @Parameter(description="Any query value")
    private String queryValue;
    
    @Nullable
    @Parameter(description="Any content type")
    @Header("Content-type")
    private String contentType;
    
    public MyRequestBean(HttpRequest<?> httpRequest, String pathVariable, String queryValue, String contentType) {
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

@javax.inject.Singleton
class MyBean {}

''')

            OpenAPI openAPI = AbstractOpenApiVisitor.testReference
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
            operation.parameters[2].name == 'Content-type'
            operation.parameters[2].description == 'Any content type'
            operation.parameters[2].in == 'header'
            !operation.parameters[2].required
    }

}
