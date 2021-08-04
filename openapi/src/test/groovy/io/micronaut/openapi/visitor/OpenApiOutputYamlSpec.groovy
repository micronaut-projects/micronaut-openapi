package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec

class OpenApiOutputYamlSpec extends AbstractTypeElementSpec {

    void "test paths and schemas for OpenAPI are sorted"() {
        given:"An API definition"
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")

        when:
        buildBeanDefinition('test.MyBean', '''
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
    public HttpResponse<Person2> path2() {
        return null;
    }
    @Get("/path1")
    public HttpResponse<Person1> path1() {
        return null;
    }
    @Get("/")
    public HttpResponse<Person3> path() {
        return null;
    }
}
@Introspected
class Person1 {
    private String name;
    private Integer debtValue;
    private Integer totalGoals;
    public Person1(String name,
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
}
@Introspected
class Person2 {
    private String name;
    public Person2(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
@Introspected
class Person3 {
    private String name;
    public Person3(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the yaml is written"
        AbstractOpenApiVisitor.testYamlReference != null

        then:"paths are sorted and schemas are sorted"
        AbstractOpenApiEndpointVisitor.testYamlReference.contains('''\
paths:
  /endpoint1:
    get:
      operationId: getPathGet
      parameters: []
      responses:
        "200":
          description: getPathGet 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint1/path1:
    get:
      operationId: path1Get1
      parameters: []
      responses:
        "200":
          description: path1Get1 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint1/path2:
    get:
      operationId: path2Get1
      parameters: []
      responses:
        "200":
          description: path2Get1 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint2:
    get:
      operationId: pathGet1
      parameters: []
      responses:
        "200":
          description: pathGet1 200 response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Person3\'
  /endpoint2/path1:
    get:
      operationId: path1Get2
      parameters: []
      responses:
        "200":
          description: path1Get2 200 response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Person1\'
  /endpoint2/path2:
    get:
      operationId: path2Get2
      parameters: []
      responses:
        "200":
          description: path2Get2 200 response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Person2\'
  /endpoint3:
    get:
      operationId: pathGet2
      parameters: []
      responses:
        "200":
          description: pathGet2 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint3/path1:
    get:
      operationId: path1Get3
      parameters: []
      responses:
        "200":
          description: path1Get3 200 response
          content:
            application/json:
              schema:
                type: string
  /endpoint3/path2:
    get:
      operationId: path2Get3
      parameters: []
      responses:
        "200":
          description: path2Get3 200 response
          content:
            application/json:
              schema:
                type: string
components:
  schemas:
    Person1:
      required:
      - debtValue
      - name
      - totalGoals
      type: object
      properties:
        name:
          type: string
        debtValue:
          type: integer
          format: int32
        totalGoals:
          type: integer
          format: int32
    Person2:
      required:
      - name
      type: object
      properties:
        name:
          type: string
    Person3:
      required:
      - name
      type: object
      properties:
        name:
          type: string''')
    }

}
