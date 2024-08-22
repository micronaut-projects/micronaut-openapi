package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI

class OpenApiDecoratorSpec extends AbstractOpenApiTypeElementSpec {

    void "test OpenApiDecorator"() {
        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.annotation.OpenAPIDecorator;
import io.swagger.v3.oas.annotations.Operation;

@OpenAPIDecorator(opIdPrefix = "cats-", opIdSuffix = "-suffix")
@Controller("/cats")
interface MyCatsOperations extends Api<MyRequest, MyResponse> {
}

@OpenAPIDecorator("dogs-")
@Controller("/dogs")
interface MyDogsOperations extends Api<MyRequest, MyResponse> {
}

@OpenAPIDecorator(opIdPrefix = "birds-", addAlways = false)
@Controller("/birds")
interface MyBirdsOperations extends Api<MyRequest, MyResponse> {
}

@OpenAPIDecorator("fishes-")
@Controller("/fishes")
interface MyFishesOperations extends Api<MyRequest, MyResponse> {

    @Get("/api/myFishesOp")
    @OpenAPIDecorator("")
    String myFishesOp();
}

interface Api<T, R> {

    @Get("/api/request")
    T getRequest();

    @Get("/api/response")
    @Operation(operationId = "getResponse")
    R getResponse();
}

class MyRequest {

    private String prop1;

    public String getProp1() {
        return prop1;
    }

    public void setProp1(String prop1) {
        this.prop1 = prop1;
    }
}

class MyResponse {

    private String prop2;

    public String getProp2() {
        return prop2;
    }

    public void setProp2(String prop2) {
        this.prop2 = prop2;
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
        openAPI.paths.size() == 9
        openAPI.paths."/cats/api/request".get.operationId == 'cats-getRequest-suffix'
        openAPI.paths."/cats/api/response".get.operationId == 'cats-getResponse-suffix'
        openAPI.paths."/dogs/api/request".get.operationId == 'dogs-getRequest'
        openAPI.paths."/dogs/api/response".get.operationId == 'dogs-getResponse'
        openAPI.paths."/birds/api/request".get.operationId == 'birds-getRequest'
        openAPI.paths."/birds/api/response".get.operationId == 'getResponse'
        openAPI.paths."/fishes/api/request".get.operationId == 'fishes-getRequest'
        openAPI.paths."/fishes/api/response".get.operationId == 'fishes-getResponse'
        openAPI.paths."/fishes/api/myFishesOp".get.operationId == 'myFishesOp'
    }

}
