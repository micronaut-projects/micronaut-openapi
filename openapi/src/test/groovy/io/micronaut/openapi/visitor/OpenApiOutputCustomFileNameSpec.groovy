package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec

class OpenApiOutputCustomFileNameSpec extends AbstractOpenApiTypeElementSpec {

    def setup() {
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FILENAME, "my-openapi-\${version}")
    }

    def cleanup() {
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_FILENAME);
    }

    void "test default filename"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

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
    Person1(String name,
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
    Person2(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
@Introspected
class Person3 {
    private String name;
    Person3(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testFileName
        Utils.testFileName == 'my-openapi-0.0.yml'
    }

    void "test default filename with placeholders"() {
        given:
        System.setProperty("version", "my-version")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

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
    Person1(String name,
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
    Person2(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
@Introspected
class Person3 {
    private String name;
    Person3(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testFileName
        Utils.testFileName == 'my-openapi-my-version.yml'

        cleanup:
        System.clearProperty("version")
    }
}
