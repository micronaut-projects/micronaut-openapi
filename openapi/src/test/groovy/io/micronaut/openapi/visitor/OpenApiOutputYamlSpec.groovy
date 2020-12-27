package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.inject.BeanDefinition

class OpenApiOutputYamlSpec extends AbstractTypeElementSpec {

    void "test paths and schemas for OpenAPI are sorted"() {
        given:"An API definition"
            System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")

        when:
            BeanDefinition beanDefinition = buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.management.endpoint.annotation.Endpoint;
import io.micronaut.management.endpoint.annotation.Delete;
import io.micronaut.management.endpoint.annotation.Write;
import io.micronaut.management.endpoint.annotation.Selector;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.servers.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


@OpenAPIDefinition(
        info = @Info(
                title = "the title",
                version = "0.0",
                description = "My API"
        )
)
class Application {

}
@Controller("/endpoint3")
class ThirdEndpointController {
    @Get("/")
    public HttpResponse<String> path() {
        return null;
    }
    @Get("/path2")
    public HttpResponse<String> path2() {
        return null;
    }
    @Get("/path1")
    public HttpResponse<String> path1() {
        return null;
    }
}
@Controller("/endpoint1")
class FirstEndpointController {
    @Get("/")
    public HttpResponse<String> getPath() {
        return null;
    }
    @Get("/path1")
    public HttpResponse<String> path1() {
        return null;
    }
    @Get("/path2")
    public HttpResponse<String> path2() {
        return null;
    }
}
@Controller("/endpoint2")
class SecondEndpointController {
    @Get("/path2")
    public HttpResponse<String> path2() {
        return null;
    }
    @Get("/path1")
    public HttpResponse<String> path1() {
        return null;
    }
    @Get("/")
    public HttpResponse<Person> path() {
        return null;
    }
}
@Introspected
class Person {
    private String name;
    private Integer debtValue;
    private Integer totalGoals;
    public Person(String name,
                  Integer debtValue,
                  Integer totalGoals) {
        this.name = name;
        this.debtValue = debtValue;
        this.totalGoals = totalGoals;
    }
    public String getName() {
        return name;
    }
    public Integer getDebtValue() {
        return debtValue;
    }
    public Integer getTotalGoals() {
        return totalGoals;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setDebtValue(Integer debtValue) {
        this.debtValue = debtValue;
    }
    public void setTotalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals;
    }
}
@javax.inject.Singleton
class MyBean {}
''')
        then:"the yaml is written"
            AbstractOpenApiVisitor.testYamlReference != null

        then:"paths are sorted and schemas are sorted"
            AbstractOpenApiEndpointVisitor.testYamlReference.contains('''\
paths:
  /endpoint1:
    get:
      operationId: getPath
      parameters: []
      responses:
        "200":
          description: getPath 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint1/path1:
    get:
      operationId: path1
      parameters: []
      responses:
        "200":
          description: path1 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint1/path2:
    get:
      operationId: path2
      parameters: []
      responses:
        "200":
          description: path2 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint2:
    get:
      operationId: path
      parameters: []
      responses:
        "200":
          description: path 200 response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Person\'
  /endpoint2/path1:
    get:
      operationId: path1
      parameters: []
      responses:
        "200":
          description: path1 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint2/path2:
    get:
      operationId: path2
      parameters: []
      responses:
        "200":
          description: path2 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint3:
    get:
      operationId: path
      parameters: []
      responses:
        "200":
          description: path 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint3/path1:
    get:
      operationId: path1
      parameters: []
      responses:
        "200":
          description: path1 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint3/path2:
    get:
      operationId: path2
      parameters: []
      responses:
        "200":
          description: path2 200 response
          content:
            application/json:
              schema:
                type: string
components:
  schemas:
    Person:
      type: object
      properties:
        debtValue:
          type: integer
          format: int32
        name:
          type: string
        totalGoals:
          type: integer
          format: int32''')
    }

}
