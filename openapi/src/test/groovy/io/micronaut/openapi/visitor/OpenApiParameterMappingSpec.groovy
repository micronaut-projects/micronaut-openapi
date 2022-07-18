package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.HeaderParameter
import spock.lang.Issue

class OpenApiParameterMappingSpec extends AbstractOpenApiTypeElementSpec {

    void "test that @Parameter propagates correctly"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import java.util.List;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import com.fasterxml.jackson.annotation.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/networks")
interface NetworkOperations {

    /**
     * @param fooBar some other description
     */
   @Operation(
            summary = "Gets mappings from TTT using vod provider mappings",
            description = "Migration of /networks endpoint from TTT. Gets mappings from XYZ using provider mappings",
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Successfully got abc data from TTT",
                            content = {
                                    @Content(
                                            mediaType = "application/json", schema = @Schema(implementation = Greeting.class)
                                    )
                            }
                    )
            })
    @Get
    public HttpResponse<Greeting> getNetworks(
            @Parameter(
                    name = "fooBar",
                    description = "NA/true/false (case insensitive)",
                    required = false,
                    schema = @Schema(
                            nullable = true,
                            type = "string", allowableValues = {"NA", "true", "false"},
                            defaultValue = "NA",
                            example = "NA"
                    )
            )
            @QueryValue(value = "fooBar", defaultValue = "NA") String fooBar
    );
}

class Greeting {
    public String message;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        Utils.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema greetingSchema = openAPI.components.schemas['Greeting']

        then:"the components are valid"
        greetingSchema.type == 'object'
        greetingSchema.properties.size() == 1
        greetingSchema.properties['message'].type == 'string'

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/networks")

        then:"it is included in the OpenAPI doc"
        pathItem.get.operationId == 'getNetworks'
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name =='fooBar'
        pathItem.get.parameters[0].description == 'NA/true/false (case insensitive)'
        !pathItem.get.parameters[0].required
        pathItem.get.parameters[0].schema.type == 'string'
        pathItem.get.parameters[0].schema.default == 'NA'
        pathItem.get.parameters[0].schema.enum == ['NA', 'true', 'false']
        pathItem.get.parameters[0].schema.example == 'NA'
    }

    void "test that @Parameter elements can be hidden on interface"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import java.util.List;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import com.fasterxml.jackson.annotation.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/networks")
interface NetworkOperations {

    @Get
    public HttpResponse<Greeting> getNetworks(
            @Parameter(hidden=true) java.security.Principal auth,
            @QueryValue(value = "fooBar", defaultValue = "NA") String fooBar
    );
}

class Greeting {
    public String message;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        Utils.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema greetingSchema = openAPI.components.schemas['Greeting']

        then:"the components are valid"
        greetingSchema.type == 'object'
        greetingSchema.properties.size() == 1
        greetingSchema.properties['message'].type == 'string'

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/networks")

        then:"it is included in the OpenAPI doc"
        pathItem.get.operationId == 'getNetworks'
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name =='fooBar'
    }

    void "test that @Parameter elements can be hidden on type"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import java.util.List;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import com.fasterxml.jackson.annotation.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/networks")
interface NetworkOperations {

    @Post
    public HttpResponse<Greeting> getNetworks(
            @Parameter(hidden=true) Greeting auth,
            @QueryValue(value = "fooBar", defaultValue = "NA") String fooBar
    );
}

class Greeting {
    public String message;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        Utils.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema greetingSchema = openAPI.components.schemas['Greeting']

        then:"the components are valid"
        greetingSchema.type == 'object'
        greetingSchema.properties.size() == 1
        greetingSchema.properties['message'].type == 'string'

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/networks")

        then:"it is included in the OpenAPI doc"
        pathItem.post.operationId == 'getNetworks'
        pathItem.post.parameters.size() == 1
        pathItem.post.parameters[0].name =='fooBar'
    }

    void "test prinicipal is not included"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import java.util.List;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import com.fasterxml.jackson.annotation.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/networks")
interface NetworkOperations {

    @Post
    public HttpResponse<Greeting> saveNetwork(
            java.security.Principal auth,
            String name
    );
}

class Greeting {
    public String message;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        Utils.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema greetingSchema = openAPI.components.schemas['Greeting']

        then:"the components are valid"
        greetingSchema.type == 'object'
        greetingSchema.properties.size() == 1
        greetingSchema.properties['message'].type == 'string'

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/networks")

        then:"it is included in the OpenAPI doc"
        pathItem.post.operationId == 'saveNetwork'
        !pathItem.post.parameters
        pathItem.post.requestBody.content['application/json'].schema
        pathItem.post.requestBody.content['application/json'].schema.properties.size() == 1
        pathItem.post.requestBody.content['application/json'].schema.properties['name']
    }

    void "test body is not included"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;

/**
 * @author HoaBo
 * @since 1.0
 */
@Controller("/networks")
interface NetworkOperations {

    @Get
    public HttpResponse getNetworks(
            java.security.Principal auth,
            @Parameter(hidden=true) String fooBar
    );
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        Utils.testReference != null

        OpenAPI openAPI = Utils.testReference

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/networks")

        then:"it is included in the OpenAPI doc"
        pathItem.get.operationId == 'getNetworks'
        !pathItem.get.parameters
        pathItem.get.requestBody == null
    }

    void "test parameter with no bindable annotations or reserved types"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Nullable;
import java.security.Principal;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/")
interface Test {

    @Get("/test1")
    public String test1(String name);

    @Post("/test2")
    public String test2(String name);

    @Get("/test3")
    public String test3(Principal principal);

    @Get("/test4")
    public String test4(HttpRequest req);

    @Get("/test5")
    public String test5(HttpRequest req, Principal principal, String name, String greeting);

    @Get("/test6{?bar}")
    public String test6(@Nullable String bar, String name);

    @Post("/test7")
    public String test7(String someId, @Nullable String someNotRequired, java.util.Optional<String> someNotRequired2, HttpRequest req, Principal principal, @Body Greeting myBody);
}

class Greeting {
    public String message;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        Utils.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        PathItem pathItem = openAPI.paths.get("/test1")

        then:
        pathItem.get.operationId == 'test1'
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name == 'name'
        pathItem.get.parameters[0].in == 'query'

        when:
        pathItem = openAPI.paths.get("/test2")

        then:
        pathItem.post.operationId == 'test2'
        !pathItem.post.parameters
        pathItem.post.requestBody.content['application/json'].schema
        pathItem.post.requestBody.content['application/json'].schema.properties.size() == 1
        pathItem.post.requestBody.content['application/json'].schema.properties['name']

        when:
        pathItem = openAPI.paths.get("/test3")

        then:
        pathItem.get.operationId == 'test3'
        !pathItem.get.parameters

        when:
        pathItem = openAPI.paths.get("/test4")

        then:
        pathItem.get.operationId == 'test4'
        !pathItem.get.parameters

        when:
        pathItem = openAPI.paths.get("/test5")

        then:
        pathItem.get.operationId == 'test5'
        pathItem.get.parameters.size() == 2
        pathItem.get.parameters[0].name == 'name'
        pathItem.get.parameters[0].in == 'query'
        pathItem.get.parameters[1].name == 'greeting'
        pathItem.get.parameters[1].in == 'query'

        when:
        pathItem = openAPI.paths.get("/test6")

        then:
        pathItem.get.operationId == 'test6'
        pathItem.get.parameters.size() == 2
        pathItem.get.parameters[0].name == 'bar'
        pathItem.get.parameters[0].in == 'query'
        pathItem.get.parameters[1].name == 'name'
        pathItem.get.parameters[1].in == 'query'

        when:
        pathItem = openAPI.paths.get("/test7")

        then:
        pathItem.post.operationId == 'test7'
        !pathItem.post.parameters
        pathItem.post.requestBody.required
        pathItem.post.requestBody.content['application/json'].schema
        pathItem.post.requestBody.content['application/json'].schema.allOf[0].$ref == "#/components/schemas/Greeting"
        pathItem.post.requestBody.content['application/json'].schema.allOf[1].properties.size() == 3
        pathItem.post.requestBody.content['application/json'].schema.allOf[1].properties['someId']
        pathItem.post.requestBody.content['application/json'].schema.allOf[1].properties['someId'].nullable == null
        pathItem.post.requestBody.content['application/json'].schema.allOf[1].properties['someNotRequired']
        pathItem.post.requestBody.content['application/json'].schema.allOf[1].properties['someNotRequired'].nullable == true
        pathItem.post.requestBody.content['application/json'].schema.allOf[1].properties['someNotRequired2']
        pathItem.post.requestBody.content['application/json'].schema.allOf[1].properties['someNotRequired2'].nullable == true
    }

    void "test @Parameter in header and explode is true"() {

        given:"An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import java.util.List;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import com.fasterxml.jackson.annotation.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/networks")
interface NetworkOperations {

    /**
     * @param fooBar some other description
     */
   @Operation(
            summary = "Gets mappings from TTT using vod provider mappings",
            description = "Migration of /networks endpoint from TTT. Gets mappings from XYZ using provider mappings",
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Successfully got abc data from TTT",
                            content = {
                                    @Content(
                                            mediaType = "application/json", schema = @Schema(implementation = Greeting.class)
                                    )
                            }
                    )
            })
    @Get
    public HttpResponse<Greeting> getNetworks(
            @Parameter(
                    in = ParameterIn.HEADER,
                    name = "fooBar",
                    description = "NA/true/false (case insensitive)",
                    required = false,
                    explode = Explode.TRUE,
                    schema = @Schema(
                            implementation = Greeting.class
                    )
            )
            Greeting fooBar
    );
}

class Greeting {
    public String message;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:"the state is correct"
        Utils.testReference != null

        when:"The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Schema greetingSchema = openAPI.components.schemas['Greeting']

        then:"the components are valid"
        greetingSchema.type == 'object'
        greetingSchema.properties.size() == 1
        greetingSchema.properties['message'].type == 'string'

        when:"the /pets path is retrieved"
        PathItem pathItem = openAPI.paths.get("/networks")

        then:"it is included in the OpenAPI doc"
        pathItem.get.operationId == 'getNetworks'
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name =='fooBar'
        pathItem.get.parameters[0].in == 'header'
        pathItem.get.parameters[0].explode
        pathItem.get.parameters[0].description == 'NA/true/false (case insensitive)'
        !pathItem.get.parameters[0].required
        pathItem.get.parameters[0].schema.$ref == '#/components/schemas/Greeting'
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/416")
    void "test that @Parameter can be applied to a method"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.*;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.*;

import java.lang.annotation.*;

@Controller("/")
class ParameterController {

    @Get("/page-params")
    @PageParam
    public String paginationParams(@Parameter(hidden = true) Page page) {
        return null;
    }
}

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Parameter(name = "page", in = ParameterIn.QUERY, description = "The page number", required = true, example = "1")
@Parameter(name = "pageSize", in = ParameterIn.QUERY)
@interface PageParam {
}

@Introspected
class Page {
    private String page;
    private String pageSize;

    public Page() {
    }
    public String getPage() {
        return page;
    }
    public void setPage(String page) {
        this.page = page;
    }
    public String getPageSize() {
        return pageSize;
    }
    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then: 'the state is correct'
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        PathItem pathItem = openAPI.paths.get("/page-params")

        then:
        pathItem.get.operationId == 'paginationParams'
        pathItem.get.parameters.size() == 2
        pathItem.get.parameters[0].name == 'page'
        pathItem.get.parameters[0].in == 'query'
        pathItem.get.parameters[0].description == 'The page number'
        pathItem.get.parameters[0].required
        pathItem.get.parameters[0].example == '1'
        pathItem.get.parameters[0].schema != null
        pathItem.get.parameters[1].name == 'pageSize'
        pathItem.get.parameters[1].in == 'query'
        pathItem.get.parameters[1].schema != null
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/416")
    void "do not duplicate parameters if defined as method arguments and also in @Parameter at method level"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.*;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.*;

import java.lang.annotation.*;

@Controller("/")
class ParameterController {

    @Get("/{apiVersion}")
    @Parameters(value = {
            @Parameter(required = true, in = ParameterIn.PATH, name = "apiVersion"),
            @Parameter(in = ParameterIn.QUERY, name = "text")
    })
    public void search(String apiVersion, @Nullable String text, @Nullable Integer page) {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then: 'the state is correct'
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        PathItem pathItem = openAPI.paths.get("/{apiVersion}")

        then:
        pathItem.get.operationId == 'search'
        pathItem.get.parameters.size() == 3
        pathItem.get.parameters[0].name == 'apiVersion'
        pathItem.get.parameters[0].in == 'path'
        pathItem.get.parameters[0].required
        pathItem.get.parameters[1].name == 'text'
        pathItem.get.parameters[1].in == 'query'
        pathItem.get.parameters[2].name == 'page'
        pathItem.get.parameters[2].in == 'query'
    }

}
