package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Issue
import spock.util.environment.RestoreSystemProperties

class OpenApiControllerVisitorSpec extends AbstractOpenApiTypeElementSpec {

    void "test some ignored parameters"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.Map;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.session.annotation.SessionValue;

@Controller
class ControllerThree {

    @Get("/myObj")
    @SessionValue("myAttr")
    MyObj myMethod(
            @Header("my-header") String header,
            @Header Map<String, String> allHeaders,
            @SessionValue @Nullable MyObj myObj) {
        return null;
    }
}

class MyObj {

    public String myProp;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def operation = Utils.testReference?.paths?."/myObj"?.get

        then:
        operation
        operation.parameters
        operation.parameters.size() == 1
        operation.parameters[0].name == "my-header"
        operation.parameters[0].in == "header"
    }

    void "test hidden endpoint with inheritance"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Hidden;

@Controller
class ControllerThree extends AbstractController {

    @Hidden
    @Override
    void methodTwo() {
    }

    void methodFour() {
    }

    @Get("/five")
    void methodFive() {
    }
}

@Hidden
class AbstractController {
    @Get("/should-be-hidden/one")
    void methodOne() {
    }

    @Get("/should-be-hidden/two")
    void methodTwo() {
    }

    void methodThree() {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Paths paths = Utils.testReference?.paths

        then:
        paths
        paths.size() == 1
        paths.containsKey("/five")
    }

    void "test java.util collections"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.util.*;
import java.util.concurrent.*;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller
class HelloWorldController {

    @Post("/ArrayList")
    public ArrayList<AB> ArrayList() {
        return null;
    }

    @Post("/ArrayDeque")
    public ArrayDeque<AB> ArrayDeque() {
        return null;
    }

    @Post("/LinkedBlockingDeque")
    public LinkedBlockingDeque<AB> LinkedBlockingDeque() {
        return null;
    }

    @Post("/SynchronousQueue")
    public SynchronousQueue<AB> SynchronousQueue() {
        return null;
    }
}

class AB {
    public String someField;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Paths paths = Utils.testReference?.paths
        Map<String, Schema> schemas = Utils.testReference?.components?.schemas

        then:
        paths
        paths.size() == 4

        paths."/ArrayList"
        paths."/ArrayList".post.operationId == 'ArrayList'
        paths."/ArrayList".post.responses."200".content."application/json".schema.type == 'array'
        paths."/ArrayList".post.responses."200".content."application/json".schema.items.$ref == '#/components/schemas/AB'

        paths."/ArrayDeque"
        paths."/ArrayDeque".post.operationId == 'ArrayDeque'
        paths."/ArrayDeque".post.responses."200".content."application/json".schema.type == 'array'
        paths."/ArrayDeque".post.responses."200".content."application/json".schema.items.$ref == '#/components/schemas/AB'

        paths."/LinkedBlockingDeque"
        paths."/LinkedBlockingDeque".post.operationId == 'LinkedBlockingDeque'
        paths."/LinkedBlockingDeque".post.responses."200".content."application/json".schema.type == 'array'
        paths."/LinkedBlockingDeque".post.responses."200".content."application/json".schema.items.$ref == '#/components/schemas/AB'

        paths."/SynchronousQueue"
        paths."/SynchronousQueue".post.operationId == 'SynchronousQueue'
        paths."/SynchronousQueue".post.responses."200".content."application/json".schema.type == 'array'
        paths."/SynchronousQueue".post.responses."200".content."application/json".schema.items.$ref == '#/components/schemas/AB'

        schemas
        schemas.size() == 1

        schemas.AB.type == 'object'
        schemas.AB.properties.someField.type == 'string'
    }

    void "test hidden endpoint"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.context.annotation.Parameter;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import java.util.List;

@Controller
class HelloWorldController {

    @Post("/public")
    public HttpResponse<String> publicEndpoint() {
        return null;
    }

    @Hidden
    @Post("/private")
    public HttpResponse<String> internalEndpoint() {
        return null;
    }

    @Operation(hidden = true)
    @Post("/private-operation")
    public HttpResponse<String> internalEndpoint2() {
        return null;
    }

    @Post("/public2")
    public HttpResponse<String> endpointWithHiddenParam(@io.swagger.v3.oas.annotations.Parameter(hidden = true) @Parameter String param1) {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Paths paths = Utils.testReference?.paths

        then:
        paths != null
        paths.size() == 2
        paths.containsKey("/public")
        !paths.containsKey("/private")
        !paths.containsKey("/private-operation")

        paths.containsKey("/public2")
        !paths."/public2".parameters
        !paths."/public2".post.parameters
    }

    void "test Inherited Controller Annotations - Issue #157"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import java.util.List;

@Tag(name = "HelloWorld")
interface HelloWorldApi {
    @Get("/")
    @Produces(MediaType.TEXT_PLAIN)
    @Tag(name = "Article Operations")
    @Operation(summary = "Get a message", description = "Returns a simple hello world.")
    @ApiResponse(responseCode = "200", description = "All good.")
    HttpResponse<String> helloWorld();
}

@Controller("/hello")
class HelloWorldController implements HelloWorldApi {
    @Override
    public HttpResponse<String> helloWorld() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?.get("/hello")?.get

        then:
        operation != null
        operation.operationId == 'helloWorld'
        !operation.parameters
        operation.tags
        operation.tags.size() == 2
        operation.tags.contains("HelloWorld")
        operation.tags.contains("Article Operations")
    }

    void "test Inherited Annotations - Issue #157"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import com.fasterxml.jackson.core.*;
import io.micronaut.http.hateoas.*;
import java.util.List;
import jakarta.validation.constraints.*;

class Pet {
    private String name;

    public String getName() { return this.name; }

    public void setName(String name) { this.name = name; }
}

interface PetOperations {
    @Post
    @Operation(summary = "This method creates a new Pet. " +
            " A successful request returns the response code 200." )
    @Tag(name = "Pet Operations")
    @RequestBody(description = "A Pet as Json")
    @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = Pet.class)))
    HttpResponse<Pet> createPet(@Body Pet pet);
}

@Controller("/api/pet")
class PetController implements PetOperations {
    @Override
    public HttpResponse<Pet> createPet(final Pet pet) {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?.get("/api/pet")?.post

        then:
        operation != null
        operation.operationId == 'createPet'
        !operation.parameters
        operation.requestBody

        when:
        def requestBody = operation.requestBody

        then:
        requestBody.description == 'A Pet as Json'
        requestBody.required == true
        requestBody.content['application/json']
        requestBody.content['application/json'].schema.$ref == '#/components/schemas/Pet'
    }

    void "test build OpenAPI doc with @Error"() {

        // TODO: currently the @Error is just ignored, consider adding to OpenApi.components.responses in the future
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import com.fasterxml.jackson.core.*;
import io.micronaut.http.hateoas.*;
import java.util.List;
import jakarta.validation.constraints.*;

@Controller("/")
class MyController {

    @Get("/subscription/{subscriptionId}")
    public String getSubscription( @Size(min=10, max=20) java.util.List<String> subscriptionId) {
        return null;
     }

    @io.micronaut.http.annotation.Error
    public HttpResponse<JsonError> jsonError(HttpRequest<?> request, JsonParseException jsonParseException) {
        JsonError error = new JsonError("Invalid JSON: " + jsonParseException.getMessage())
                .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<JsonError>status(HttpStatus.BAD_REQUEST, "Fix Your JSON")
                .body(error);
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?.get("/subscription/{subscriptionId}")?.get

        then:
        operation != null
        operation.operationId == 'getSubscription'
        operation.parameters.size() == 1

        when:
        def parameter = operation.parameters[0]

        then:
        parameter.in == 'path'
        parameter.schema.maxLength == null
        parameter.schema.minLength == null
        parameter.schema.minItems == 10
        parameter.schema.maxItems == 20
    }

    void "test build OpenAPI doc with @Content without mediaType information"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.micronaut.http.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author graemerocher
 * @since 1.0
 */

@Controller("/pets")
interface PetOperations<T extends Pet> {

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @ApiResponse(
        responseCode = "200",
        description = "Get Pet",
        content = @Content(schema = @Schema(implementation = Pet.class))
    )
    @Get("/{slug}")
    HttpResponse<T> find(String slug, HttpRequest request);
}

class Pet {
    private String name;

    public String getName() { return this.name; }

    public void setName(String name) { this.name = name; }
}


@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "the /pets path is retrieved"
        OpenAPI openAPI = Utils.testReference

        and: "the /{slug} path is retrieved"
        PathItem pathItem = openAPI.paths.get("/pets/{slug}")

        then: "it is included in the OpenAPI doc"
        pathItem.get.description == 'Find a pet by a slug'
        pathItem.get.operationId == 'find'
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name == 'slug'
        pathItem.get.parameters[0].in == ParameterIn.PATH.toString()
        pathItem.get.parameters[0].required
        pathItem.get.parameters[0].schema
        pathItem.get.parameters[0].description == 'The slug name'
        pathItem.get.parameters[0].schema.type == 'string'
        pathItem.get.responses.size() == 1
        pathItem.get.responses['200'] != null
        pathItem.get.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/Pet'
    }

    void "test build OpenAPI doc with request and response"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.micronaut.http.*;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends String> {

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get("/")
    HttpResponse<Single<List<T>>> list();

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @Get("/{slug}")
    HttpResponse<T> find(String slug, HttpRequest request);

    @Get("/extras/{extraId}")
    HttpResponse<T> findExtras(@PathVariable String extraId, HttpRequest request);

    @Get("/random")
    HttpResponse<T> getRandomPet();
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "the /pets path is retrieved"
        OpenAPI openAPI = Utils.testReference
        PathItem pathItem = openAPI.paths.get("/pets")

        then: "it is included in the OpenAPI doc"
        pathItem.get.operationId == 'list'
        pathItem.get.description == 'List the pets'
        pathItem.get.responses['200']
        pathItem.get.responses['200'].description == 'a list of pet names'
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.type == 'array'

        when: "the /{slug} path is retrieved"
        pathItem = openAPI.paths.get("/pets/{slug}")

        then: "it is included in the OpenAPI doc"
        pathItem.get.description == 'Find a pet by a slug'
        pathItem.get.operationId == 'find'
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name == 'slug'
        pathItem.get.parameters[0].in == ParameterIn.PATH.toString()
        pathItem.get.parameters[0].required
        pathItem.get.parameters[0].schema
        pathItem.get.parameters[0].description == 'The slug name'
        pathItem.get.parameters[0].schema.type == 'string'
        pathItem.get.responses.size() == 1
        pathItem.get.responses['200'] != null
        pathItem.get.responses['200'].content['application/json'].schema.type == 'string'

        when: "the /extras/{extraId} path is retrieved"
        pathItem = openAPI.paths.get("/pets/extras/{extraId}")

        then: "it is included in the OpenAPI doc"
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name == 'extraId'
        pathItem.get.parameters[0].in == ParameterIn.PATH.toString()
        pathItem.get.parameters[0].required
        pathItem.get.parameters[0].schema
        pathItem.get.parameters[0].schema.type == 'string'
        pathItem.get.responses.size() == 1
        pathItem.get.responses['200'] != null
        pathItem.get.responses['200'].content['application/json'].schema.type == 'string'

        when: "the /getSomething path is retrieved"
        pathItem = openAPI.paths.get("/pets/random")

        then: "default response has default description"
        pathItem.get.operationId == 'getRandomPet'
        pathItem.get.responses['200'].description == 'getRandomPet 200 response'
        pathItem.get.responses['200'].content['application/json'].schema.type == 'string'

    }

    void "test build OpenAPI doc for simple type with generics"() {

        given: "An API definition"
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.MediaType;
import io.reactivex.*;
import io.micronaut.http.annotation.*;
import java.util.List;

/**
 * @author graemerocher
 * @since 1.0
 */
@Controller("/pets")
interface PetOperations<T extends String> {

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get(value = "/", produces = MediaType.APPLICATION_JSON, single = true)
    Single<List<T>> list();

    /**
     * List the pets
     *
     * @return a list of pet names
     */
    @Get("/flowable")
    Flowable<T> flowable();

    @Get("/random")
    Maybe<T> random();

    @Get("/vendor/{name}")
    Single<List<T>> byVendor(String name);

    /**
     * Find a pet by a slug
     *
     * @param slug The slug name
     * @return A pet or 404
     */
    @Get("/{slug}")
    Maybe<T> find(String slug);

    @Post("/")
    Single<T> save(@Body T pet);
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "the /pets path is retrieved"
        OpenAPI openAPI = Utils.testReference
        PathItem pathItem = openAPI.paths.get("/pets")

        then: "it is included in the OpenAPI doc"
        pathItem.get.operationId == 'list'
        pathItem.get.description == 'List the pets'
        pathItem.get.responses['200']
        pathItem.get.responses['200'].description == 'a list of pet names'
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.type == 'array'
        pathItem.post.operationId == 'save'
        pathItem.post.requestBody
        pathItem.post.requestBody.required
        pathItem.post.requestBody.content
        pathItem.post.requestBody.content.size() == 1


        when: "the /{slug} path is retrieved"
        pathItem = openAPI.paths.get("/pets/{slug}")

        then: "it is included in the OpenAPI doc"
        pathItem.get.description == 'Find a pet by a slug'
        pathItem.get.operationId == 'find'
        pathItem.get.parameters.size() == 1
        pathItem.get.parameters[0].name == 'slug'
        pathItem.get.parameters[0].in == ParameterIn.PATH.toString()
        pathItem.get.parameters[0].required
        pathItem.get.parameters[0].schema
        pathItem.get.parameters[0].description == 'The slug name'
        pathItem.get.parameters[0].schema.type == 'string'
        pathItem.get.responses.size() == 1
        pathItem.get.responses['200'] != null
        pathItem.get.responses['200'].content['application/json'].schema.type == 'string'

        when: "A flowable is returned"
        pathItem = openAPI.paths.get("/pets/flowable")

        then:
        pathItem.get.operationId == 'flowable'
        pathItem.get.responses['200']
        pathItem.get.responses['200'].description == 'a list of pet names'
        pathItem.get.responses['200'].content['application/json'].schema
        pathItem.get.responses['200'].content['application/json'].schema.type == 'array'
    }

    void "test parse custom parameter data"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Get("/subscription/{subscriptionId}")
    public String getSubscription(
               @Parameter(in = ParameterIn.PATH, name = "subscriptionId",
               required = true, description = "parameter description",
               allowEmptyValue = true, allowReserved = true,
               schema = @Schema(
                                    type = "string",
                                    format = "uuid",
                                    description = "the generated UUID")) String subscriptionId) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        Operation operation = Utils.testReference?.paths?.get("/subscription/{subscriptionId}")?.get

        expect:
        operation != null
        operation.operationId == 'getSubscription'
        operation.parameters.size() == 1
        operation.parameters[0].in == 'path'
        operation.parameters[0].name == 'subscriptionId'
        operation.parameters[0].description == 'parameter description'
        operation.parameters[0].required
        operation.parameters[0].allowEmptyValue
        operation.parameters[0].allowReserved
        operation.parameters[0].schema.type == 'string'
        operation.parameters[0].schema.format == 'uuid'
        operation.parameters[0].schema.description == 'the generated UUID'

    }

    void "test parse jakarta.validation constraints for String"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import jakarta.validation.constraints.*;

@Controller("/")
class MyController {

    @Get("/subscription/{subscriptionId}")
    public String getSubscription( @NotBlank @Max(10) @Min(5) @Pattern(regexp="xxxxx") @Size(min=10, max=20) String subscriptionId) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?.get("/subscription/{subscriptionId}")?.get

        then:
        operation != null
        operation.operationId == 'getSubscription'
        operation.parameters.size() == 1


        when:
        def parameter = operation.parameters[0]

        then:
        parameter.in == 'path'
        parameter.schema.maxLength == 20
        parameter.schema.minLength == 10
        parameter.schema.pattern == 'xxxxx'
        parameter.schema.maximum == 10
        parameter.schema.minimum == 5

    }

    void "test parse jakarta.validation constraints for String[]"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import jakarta.validation.constraints.*;

@Controller("/")
class MyController {

    @Get("/subscription/{subscriptionId}")
    public String getSubscription( @Size(min=10, max=20) String[] subscriptionId) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?.get("/subscription/{subscriptionId}")?.get

        then:
        operation != null
        operation.operationId == 'getSubscription'
        operation.parameters.size() == 1


        when:
        def parameter = operation.parameters[0]

        then:
        parameter.in == 'path'
        parameter.schema.maxLength == null
        parameter.schema.minLength == null
        parameter.schema.minItems == 10
        parameter.schema.maxItems == 20

    }

    void "test parse jakarta.validation constraints for List"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import jakarta.validation.constraints.*;

@Controller("/")
class MyController {

    @Get("/subscription/{subscriptionId}")
    public String getSubscription( @Size(min=10, max=20) java.util.List<String> subscriptionId) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?.get("/subscription/{subscriptionId}")?.get

        then:
        operation != null
        operation.operationId == 'getSubscription'
        operation.parameters.size() == 1


        when:
        def parameter = operation.parameters[0]

        then:
        parameter.in == 'path'
        parameter.schema.maxLength == null
        parameter.schema.minLength == null
        parameter.schema.minItems == 10
        parameter.schema.maxItems == 20

    }

    void "test parse jakarta.validation.NotEmpty constraint for List"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import jakarta.validation.constraints.*;

@Controller("/")
class MyController {

    @Get("/subscription/{subscriptionId}")
    public String getSubscription( @NotEmpty java.util.List<String> subscriptionId) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?.get("/subscription/{subscriptionId}")?.get

        then:
        operation != null
        operation.operationId == 'getSubscription'
        operation.parameters.size() == 1

        when:
        def parameter = operation.parameters[0]

        then:
        parameter.in == 'path'
        parameter.schema.minItems == 1

    }

    void "test parse @Header, @CookieValue, @QueryValue parameter data"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import io.micronaut.core.annotation.Nullable;

@Controller("/")
class MyController {

    @Get("/subscription/{subscriptionId}")
    public String getSubscription(@Parameter(description="foo") @CookieValue String subscriptionId,
            @QueryValue String q,
            @Header String myContentType,
            @Nullable @Header(name = "Bundle-ID") String bundleId,
            @Header("X-API-Version") String apiVersion) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        Operation operation = Utils.testReference?.paths?.get("/subscription/{subscriptionId}")?.get

        expect:
        operation != null
        operation.operationId == 'getSubscription'
        operation.parameters.size() == 5
        operation.parameters[0].in == 'cookie'
        operation.parameters[0].name == 'subscriptionId'
        operation.parameters[0].required
        operation.parameters[0].description == 'foo'
        operation.parameters[1].in == 'query'
        operation.parameters[1].name == 'q'
        operation.parameters[1].required
        operation.parameters[2].in == 'header'
        operation.parameters[2].name == 'my-content-type'
        operation.parameters[2].required
        operation.parameters[3].in == 'header'
        operation.parameters[3].name == 'Bundle-ID'
        !operation.parameters[3].required
        operation.parameters[4].in == 'header'
        operation.parameters[4].name == 'X-API-Version'
        operation.parameters[4].required
    }

    void "test URI template with query parameters is handled correctly"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("${mycontroller.path:/}")
class MyController {

    @Get("${mymethod.path:/hello}{?foo,bar}")
    public String query(@Nullable String foo, @Nullable String bar) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?.get("/hello")?.get

        then:
        operation != null
        operation.operationId == 'query'
        operation.parameters.size() == 2
        operation.parameters[0].name == 'foo'
        !operation.parameters[0].required
        operation.parameters[0].in == 'query'
        operation.parameters[1].name == 'bar'
        !operation.parameters[1].required
        operation.parameters[1].in == 'query'

    }

    void "test operation with multiple uris - Issue #220"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import jakarta.validation.constraints.*;

@Controller("/")
class MyController {

    @Get(uris = {"/subscription/{subscriptionId}", "/subs/{subscriptionId}"})
    public String getSubscription(String[] subscriptionId) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        OpenAPI api = Utils.testReference

        then:
        api.paths.size() == 2

        when:
        Operation operation = api.paths?.get("/subscription/{subscriptionId}")?.get

        then:
        operation != null
        operation.operationId == 'getSubscription_1'
        operation.parameters.size() == 1

        when:
        def parameter = operation.parameters[0]

        then:
        parameter.in == 'path'

        when:
        operation = api.paths?.get("/subs/{subscriptionId}")?.get

        then:
        parameter.in == 'path'

        then:
        operation != null
        operation.operationId == 'getSubscription'
        operation.parameters.size() == 1

        when:
        parameter = operation.parameters[0]

        then:
        parameter.in == 'path'
    }

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/440")
    void "test return RxJava3 Single"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.*;
import io.reactivex.rxjava3.core.Single;

@Controller("/testing")
class TestController {
    @Get
    public Single<TestPojo> get() {
        return Single.just(new TestPojo("testing123"));
    }
}

class TestPojo {
    private final String testString;

    public TestPojo(String testString) {
        this.testString = testString;
    }

    public String getTestString() {
        return testString;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference

        then:
        openAPI.components.schemas.size() == 1
        openAPI.components.schemas['TestPojo'].type == 'object'
        openAPI.components.schemas['TestPojo'].properties.size() == 1
        openAPI.components.schemas['TestPojo'].properties['testString'].type == 'string'
        openAPI.components.schemas['TestPojo'].required.size() == 1
        openAPI.components.schemas['TestPojo'].required.contains('testString')
    }

    void "test ApiResponse with useReturnTypeSchema"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller
interface HelloWorldApi {
    @Get
    @ApiResponse(responseCode = "200", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "500", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_XML, schema = @Schema(implementation = String.class)))
    HttpResponse<TestPojo> helloWorld();
}

class TestPojo {

    private String testString;

    public String getTestString() {
        return testString;
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/"?.get

        then:
        operation != null
        operation.operationId == 'helloWorld'
        operation.responses.size() == 3
        operation.responses."200".content."application/json".schema.$ref == '#/components/schemas/TestPojo'
        operation.responses."500".content."application/json".schema.$ref == '#/components/schemas/TestPojo'
        operation.responses."201".content."application/xml".schema.type == 'string'
    }

    void "test ApiResponse with useReturnTypeSchema on class"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@ApiResponse(responseCode = "200", useReturnTypeSchema = true)
@ApiResponse(responseCode = "500", useReturnTypeSchema = true)
@Controller
interface HelloWorldApi {
    @Get
    @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_XML, schema = @Schema(implementation = String.class)))
    HttpResponse<TestPojo> helloWorld();
}

class TestPojo {

    private String testString;

    public String getTestString() {
        return testString;
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/"?.get

        then:
        operation != null
        operation.operationId == 'helloWorld'
        operation.responses.size() == 3
        operation.responses."200".content."application/json".schema.$ref == '#/components/schemas/TestPojo'
        operation.responses."500".content."application/json".schema.$ref == '#/components/schemas/TestPojo'
        operation.responses."201".content."application/xml".schema.type == 'string'
    }

    void "test ApiResponse with useReturnTypeSchema on class2"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
    @ApiResponse(responseCode = "200", useReturnTypeSchema = true),
    @ApiResponse(responseCode = "500", useReturnTypeSchema = true)
})
@Controller
interface HelloWorldApi {
    @Get
    @ApiResponses({
        @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_XML, schema = @Schema(implementation = String.class)))
    })
    HttpResponse<TestPojo> helloWorld();
}

class TestPojo {

    private String testString;

    public String getTestString() {
        return testString;
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/"?.get

        then:
        operation != null
        operation.operationId == 'helloWorld'
        operation.responses.size() == 3
        operation.responses."200".content."application/json".schema.$ref == '#/components/schemas/TestPojo'
        operation.responses."500".content."application/json".schema.$ref == '#/components/schemas/TestPojo'
        operation.responses."201".content."application/xml".schema.type == 'string'
    }

    void "test ApiResponse with useReturnTypeSchema2"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Controller
interface HelloWorldApi {
    @Get
    @ApiResponses({
        @ApiResponse(responseCode = "200", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "500", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_XML, schema = @Schema(implementation = String.class)))
    })
    HttpResponse<TestPojo> helloWorld();
}

class TestPojo {

    private String testString;

    public String getTestString() {
        return testString;
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/"?.get

        then:
        operation != null
        operation.operationId == 'helloWorld'
        operation.responses.size() == 3
        operation.responses."200".content."application/json".schema.$ref == '#/components/schemas/TestPojo'
        operation.responses."500".content."application/json".schema.$ref == '#/components/schemas/TestPojo'
        operation.responses."201".content."application/xml".schema.type == 'string'
    }

    void "test skip ignored headers"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;

@Controller
class HelloWorldController {

    @Post("/public")
    public HttpResponse<String> publicEndpoint(
            @Header(HttpHeaders.AUTHORIZATION) String auth,
            @Header(HttpHeaders.CONTENT_TYPE) String contentType,
            @Header(HttpHeaders.ACCEPT) String accept,
            @Header String myHeader
    ) {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/public"?.post

        then:
        operation
        operation.parameters
        operation.parameters.size() == 1
        operation.parameters[0].name == 'my-header'
    }

    void "test ApiResponse declared in interface on method level"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.reactivestreams.Publisher;

@Controller
class MyController implements MyEndpoint {

    @Get("/data")
    @Override
    public Publisher<String> getData() {
        return null;
    }
}

interface MyEndpoint {

    @ApiResponse(responseCode = "200", description = "All is ok")
    @ApiResponse(responseCode = "404", description = "Data is missing")
    Publisher<String> getData();
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/data"?.get

        then:
        operation
        operation.responses.size() == 2
        operation.responses."200".description == 'All is ok'
        operation.responses."404".description == 'Data is missing'
    }

    void "test ApiResponse declared in interface and class on method level"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.reactivestreams.Publisher;

@Controller
class MyController implements MyEndpoint {

    @Get("/data")
    @ApiResponse(responseCode = "407", description = "My response1")
    @ApiResponse(responseCode = "515", description = "My response2")
    @ApiResponse(responseCode = "200", description = "My ok")
    @Override
    public Publisher<String> getData() {
        return null;
    }
}

interface MyEndpoint {

    @ApiResponse(responseCode = "200", description = "All is ok")
    @ApiResponse(responseCode = "404", description = "Data is missing")
    Publisher<String> getData();
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/data"?.get

        then:
        operation
        operation.responses.size() == 4
        operation.responses."200".description == 'My ok'
        operation.responses."404".description == 'Data is missing'
        operation.responses."407".description == 'My response1'
        operation.responses."515".description == 'My response2'
    }

    void "test ApiResponse declared in interface on class level"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.reactivestreams.Publisher;

@Controller
class MyController implements MyEndpoint {

    @Get("/data")
    @Override
    public Publisher<String> getData() {
        return null;
    }

    @Get("/data2")
    @Override
    public Publisher<String> getData2() {
        return null;
    }
}

@ApiResponse(responseCode = "200", description = "All is ok")
@ApiResponse(responseCode = "404", description = "Data is missing")
interface MyEndpoint {

    Publisher<String> getData();

    Publisher<String> getData2();
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/data"?.get
        Operation operation2 = Utils.testReference?.paths?."/data2"?.get

        then:
        operation
        operation.responses.size() == 2
        operation.responses."200".description == 'All is ok'
        operation.responses."404".description == 'Data is missing'

        operation2
        operation2.responses.size() == 2
        operation2.responses."200".description == 'All is ok'
        operation2.responses."404".description == 'Data is missing'
    }

    void "test ApiResponse declared in interface and class on class level"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.reactivestreams.Publisher;

@ApiResponse(responseCode = "407", description = "My response1")
@ApiResponse(responseCode = "515", description = "My response2")
@ApiResponse(responseCode = "200", description = "My ok")
@Controller
class MyController implements MyEndpoint {

    @Get("/data")
    @Override
    public Publisher<String> getData() {
        return null;
    }

    @Get("/data2")
    @Override
    public Publisher<String> getData2() {
        return null;
    }
}

@ApiResponse(responseCode = "200", description = "All is ok")
@ApiResponse(responseCode = "404", description = "Data is missing")
interface MyEndpoint {

    Publisher<String> getData();

    Publisher<String> getData2();
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/data"?.get
        Operation operation2 = Utils.testReference?.paths?."/data2"?.get

        then:
        operation
        operation.responses.size() == 4
        operation.responses."200".description == 'My ok'
        operation.responses."404".description == 'Data is missing'
        operation.responses."407".description == 'My response1'
        operation.responses."515".description == 'My response2'

        operation2
        operation2.responses.size() == 4
        operation2.responses."200".description == 'My ok'
        operation2.responses."404".description == 'Data is missing'
        operation.responses."407".description == 'My response1'
        operation.responses."515".description == 'My response2'
    }

    void "test ApiResponse declared in interface and class on class and method levels"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.reactivestreams.Publisher;

@ApiResponse(responseCode = "407", description = "My response1")
@ApiResponse(responseCode = "515", description = "My response2")
@ApiResponse(responseCode = "200", description = "My ok")
@Controller
class MyController implements MyEndpoint {

    @ApiResponse(responseCode = "200", description = "Method ok")
    @ApiResponse(responseCode = "303", description = "Method redirect")
    @ApiResponse(responseCode = "501", description = "Method error")
    @Get("/data")
    @Override
    public Publisher<String> getData() {
        return null;
    }
}

@ApiResponse(responseCode = "200", description = "All is ok")
@ApiResponse(responseCode = "404", description = "Data is missing")
@ApiResponse(responseCode = "501", description = "Interface 501 code")
interface MyEndpoint {

    @ApiResponse(responseCode = "200", description = "Method ok")
    @ApiResponse(responseCode = "201", description = "Interface code")
    Publisher<String> getData();
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/data"?.get

        then:
        operation
        operation.responses.size() == 7
        // method
        operation.responses."200".description == 'Method ok'
        operation.responses."201".description == 'Interface code'
        operation.responses."303".description == 'Method redirect'
        operation.responses."501".description == 'Method error'

        // class
        operation.responses."404".description == 'Data is missing'
        operation.responses."407".description == 'My response1'
        operation.responses."515".description == 'My response2'
    }

    void "test ApiResponses declared in interface and class on class and method levels"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.reactivestreams.Publisher;

@ApiResponses({
    @ApiResponse(responseCode = "407", description = "My response1"),
    @ApiResponse(responseCode = "515", description = "My response2"),
    @ApiResponse(responseCode = "200", description = "My ok"),
})
@Controller
class MyController implements MyEndpoint {

    @ApiResponse(responseCode = "200", description = "Method ok")
    @ApiResponse(responseCode = "303", description = "Method redirect")
    @ApiResponse(responseCode = "501", description = "Method error")
    @Get("/data")
    @Override
    public Publisher<String> getData() {
        return null;
    }
}

@ApiResponse(responseCode = "200", description = "All is ok")
@ApiResponse(responseCode = "404", description = "Data is missing")
@ApiResponse(responseCode = "501", description = "Interface 501 code")
interface MyEndpoint {

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Method ok"),
        @ApiResponse(responseCode = "201", description = "Interface code"),
    })
    Publisher<String> getData();
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        Operation operation = Utils.testReference?.paths?."/data"?.get

        then:
        operation
        operation.responses.size() == 7
        // method
        operation.responses."200".description == 'Method ok'
        operation.responses."201".description == 'Interface code'
        operation.responses."303".description == 'Method redirect'
        operation.responses."501".description == 'Method error'

        // class
        operation.responses."404".description == 'Data is missing'
        operation.responses."407".description == 'My response1'
        operation.responses."515".description == 'My response2'
    }

    void "test container types"() {

        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import kotlinx.coroutines.flow.Flow;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@Controller
class HelloWorldController {

    // arrays
    // kotlin coroutines
    @Post("/endpoint11")
    public Flow<MyDto> endpoint1(@Body Flow<MyDto> body) {
        return null;
    }

    // reactivex3
    @Post("/endpoint12")
    public Flowable<MyDto> endpoint1(@Body Flowable<MyDto> body) {
        return null;
    }

    @Post("/endpoint13")
    public Observable<MyDto> endpoint1(@Body Observable<MyDto> body) {
        return null;
    }

    // reactivex2
    @Post("/endpoint14")
    public io.reactivex.Flowable<MyDto> endpoint1(@Body io.reactivex.Flowable<MyDto> body) {
        return null;
    }

    @Post("/endpoint15")
    public io.reactivex.Observable<MyDto> endpoint1(@Body io.reactivex.Observable<MyDto> body) {
        return null;
    }

    // reactor
    @Post("/endpoint16")
    public Flux<MyDto> endpoint1(@Body Flux<MyDto> body) {
        return null;
    }

    @Post("/endpoint17")
    public Publisher<MyDto> endpoint1(@Body Publisher<MyDto> body) {
        return null;
    }

    // single result
    @Post("/endpoint21")
    public Future<MyDto> endpoint1(@Body Future<MyDto> body) {
        return null;
    }

    @Post("/endpoint22")
    public CompletableFuture<MyDto> endpoint1(@Body CompletableFuture<MyDto> body) {
        return null;
    }

    @Post("/endpoint23")
    public Optional<MyDto> endpoint1(@Body Optional<MyDto> body) {
        return null;
    }

    @Post("/endpoint24")
    public Mono<MyDto> endpoint1(@Body Mono<MyDto> body) {
        return null;
    }

    @Post("/endpoint25")
    public Single<MyDto> endpoint1(@Body Single<MyDto> body) {
        return null;
    }

    @Post("/endpoint26")
    public Maybe<MyDto> endpoint1(@Body Maybe<MyDto> body) {
        return null;
    }

    @Post("/endpoint27")
    public io.reactivex.Single<MyDto> endpoint1(@Body io.reactivex.Single<MyDto> body) {
        return null;
    }

    @Post("/endpoint28")
    public io.reactivex.Maybe<MyDto> endpoint1(@Body io.reactivex.Maybe<MyDto> body) {
        return null;
    }

    @Post("/endpoint29")
    public AtomicReference<MyDto> endpoint1(@Body AtomicReference<MyDto> body) {
        return null;
    }

    @Post("/endpoint210")
    public com.google.common.base.Optional<MyDto> endpoint1(@Body com.google.common.base.Optional<MyDto> body) {
        return null;
    }
    
    @Post("/optInt")
    public OptionalInt optInt(@Body OptionalInt body) {
        return null;
    }

    @Post("/optLong")
    public OptionalLong optLong(@Body OptionalLong body) {
        return null;
    }

    @Post("/optDouble")
    public OptionalDouble optDouble(@Body OptionalDouble body) {
        return null;
    }
}

class MyDto {

    public String field;
    public OptionalInt optInt;
    public OptionalLong optLong;
    public OptionalDouble optDouble;
    public Optional<String> optStr;
    public com.google.common.base.Optional<String> guavaOptStr;
    public AtomicReference<String> referenceStr;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference

        then:

        // collection
        for (def i = 1; i < 8; i++) {
            def operation = openAPI.paths.get("/endpoint1" + i).post
            assert operation.requestBody.content."application/json".schema.items.$ref == '#/components/schemas/MyDto'
            assert operation.responses."200".content."application/json".schema.items.$ref == '#/components/schemas/MyDto'
        }

        // single
        for (def i = 1; i < 11; i++) {
            def operation = openAPI.paths.get("/endpoint2" + i).post
            assert operation.requestBody.content."application/json".schema.$ref == '#/components/schemas/MyDto'
            assert operation.responses."200".content."application/json".schema.$ref == '#/components/schemas/MyDto'
        }

        // optional primitives

        def optInt = openAPI.paths."/optInt".post
        optInt.requestBody.content."application/json".schema.type == 'integer'
        optInt.responses."200".content."application/json".schema.type == 'integer'

        def optLong = openAPI.paths."/optLong".post
        optLong.requestBody.content."application/json".schema.type == 'integer'
        optLong.requestBody.content."application/json".schema.format == 'int64'
        optLong.responses."200".content."application/json".schema.type == 'integer'
        optLong.responses."200".content."application/json".schema.format == 'int64'

        def optDouble = openAPI.paths."/optDouble".post
        optDouble.requestBody.content."application/json".schema.type == 'number'
        optDouble.requestBody.content."application/json".schema.format == 'double'
        optDouble.responses."200".content."application/json".schema.type == 'number'
        optDouble.responses."200".content."application/json".schema.format == 'double'

        def myDto = openAPI.components.schemas.MyDto
        myDto.properties
        myDto.properties.field
        myDto.properties.field.type == 'string'

        myDto.properties.optInt.type == 'integer'
        myDto.properties.optInt.format == 'int32'
        myDto.properties.optInt.nullable == true

        myDto.properties.optLong.type == 'integer'
        myDto.properties.optLong.format == 'int64'
        myDto.properties.optLong.nullable == true

        myDto.properties.optDouble.type == 'number'
        myDto.properties.optDouble.format == 'double'
        myDto.properties.optDouble.nullable == true

        myDto.properties.optStr.type == 'string'
        myDto.properties.optStr.nullable == true

        myDto.properties.guavaOptStr.type == 'string'
        myDto.properties.guavaOptStr.nullable == true

        myDto.properties.referenceStr.type == 'string'
        myDto.properties.referenceStr.nullable == true
    }

    void "test parse @Part StreamingFileUpload parameter data"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.StreamingFileUpload;
import io.swagger.v3.oas.annotations.Parameter;

import org.reactivestreams.Publisher;

@Controller("/api")
class MyController {

    @Post(uri = "/model", consumes = MediaType.MULTIPART_FORM_DATA)
    public String model(@Parameter(description = "Bind multi-part") @Part Publisher<StreamingFileUpload> files) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI?.paths?."/api/model"?.post

        then:
        operation
        operation.requestBody
        operation.requestBody.content."multipart/form-data".schema.properties.files
        operation.requestBody.content."multipart/form-data".schema.properties.files.type == "array"
        operation.requestBody.content."multipart/form-data".schema.properties.files.description == "Bind multi-part"
        operation.requestBody.content."multipart/form-data".schema.properties.files.items.type == "string"
        operation.requestBody.content."multipart/form-data".schema.properties.files.items.format == "binary"
        operation.requestBody.content."multipart/form-data".encoding.files.contentType == 'application/octet-stream'
        !operation.parameters
    }

    void "test parse @Part CompletedFileUpload parameter data"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.swagger.v3.oas.annotations.Parameter;

@Controller("/api")
class MyController {

    @Post(uri = "/model", consumes = MediaType.MULTIPART_FORM_DATA)
    public String model(
        @Parameter(description = "Bind multi-part") @Part CompletedFileUpload files
    ) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI?.paths?."/api/model"?.post

        then:
        operation
        operation.requestBody
        operation.requestBody.content."multipart/form-data".schema.properties.files
        operation.requestBody.content."multipart/form-data".schema.properties.files.type == "string"
        operation.requestBody.content."multipart/form-data".schema.properties.files.format == "binary"
        operation.requestBody.content."multipart/form-data".schema.properties.files.description == "Bind multi-part"
        operation.requestBody.content."multipart/form-data".encoding.files.contentType == 'application/octet-stream'
        !operation.parameters
    }

    void "test parse @Format ZonedDateTime parameter data"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import java.time.ZonedDateTime;

import io.micronaut.core.convert.format.Format;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Parameter;

@Controller("/api")
class MyController {

    @Post(uri = "/model")
    public String model(
        @Parameter(description = "Simple datetime param") @Format("yyyy-MM-dd'T'HH:mm[:ss][.SSS][XXX]") ZonedDateTime date
    ) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI?.paths?."/api/model"?.post

        then:
        operation
        operation.requestBody
        operation.requestBody.content."application/json".schema.properties.date
        operation.requestBody.content."application/json".schema.properties.date.type == "string"
        operation.requestBody.content."application/json".schema.properties.date.format == "date-time"
        operation.requestBody.content."application/json".schema.properties.date.description == "Simple datetime param"
        !operation.parameters
    }

    void "test parameter examples on method level"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@Controller
class MyController {

    @Parameter(name = "param",
        examples = {
            @ExampleObject(
                    name = "example1",
                    summary = "Ex1 summary",
                    description = "Ex1 description",
                    value = "{\\"p1\\": \\"v1\\", \\"p2\\": 123}"
            ),
            @ExampleObject(
                    name = "example2",
                    summary = "Ex2 summary",
                    description = "Ex2 description",
                    value = "{\\"p21\\": \\"v1\\", \\"p22\\": 123}"
            ),
    })
    @Post("/model")
    public String model(@QueryValue String param
    ) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI?.paths?."/model"?.post

        then:
        operation
        def examples = operation.parameters[0].examples
        examples.size() == 2
        examples.example1
        examples.example1.summary == "Ex1 summary"
        examples.example1.description == "Ex1 description"
        examples.example1.value.p1 == "v1"
        examples.example1.value.p2 == 123

        examples.example2
        examples.example2.summary == "Ex2 summary"
        examples.example2.description == "Ex2 description"
        examples.example2.value.p21 == "v1"
        examples.example2.value.p22 == 123
    }

    void "test parameter examples on parameter level"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@Controller
class MyController {

    @Post("/model")
    public String model(
        @Parameter(
        examples = {
            @ExampleObject(
                    name = "example1",
                    summary = "Ex1 summary",
                    description = "Ex1 description",
                    value = "{\\"p1\\": \\"v1\\", \\"p2\\": 123}"
            ),
            @ExampleObject(
                    name = "example2",
                    summary = "Ex2 summary",
                    description = "Ex2 description",
                    value = "{\\"p21\\": \\"v1\\", \\"p22\\": 123}"
            ),
    }) @QueryValue String param
    ) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI?.paths?."/model"?.post

        then:
        operation
        def examples = operation.parameters[0].examples
        examples.size() == 2
        examples.example1
        examples.example1.summary == "Ex1 summary"
        examples.example1.description == "Ex1 description"
        examples.example1.value.p1 == "v1"
        examples.example1.value.p2 == 123

        examples.example2
        examples.example2.summary == "Ex2 summary"
        examples.example2.description == "Ex2 description"
        examples.example2.value.p21 == "v1"
        examples.example2.value.p22 == 123
    }

    void "test api response header examples"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller
class MyController {

    @Operation(description = "method", responses = {
            @ApiResponse(headers = {
                    @Header(
                            name = "header2",
                            description = "header 2",
                            examples = {
                                @ExampleObject(
                                        name = "example1",
                                        summary = "Ex1 summary",
                                        description = "Ex1 description",
                                        value = "{\\"p1\\": \\"v1\\", \\"p2\\": 123}"
                                ),
                                @ExampleObject(
                                        name = "example2",
                                        summary = "Ex2 summary",
                                        description = "Ex2 description",
                                        value = "{\\"p21\\": \\"v1\\", \\"p22\\": 123}"
                                )
                            }
                    ),
            })
    })
    @Post("/model")
    public String model(@QueryValue String param) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI?.paths?."/model"?.post

        then:
        operation
        def examples = operation.responses.'200'.headers.header2.examples
        examples.size() == 2
        examples.example1
        examples.example1.summary == "Ex1 summary"
        examples.example1.description == "Ex1 description"
        examples.example1.value.p1 == "v1"
        examples.example1.value.p2 == 123

        examples.example2
        examples.example2.summary == "Ex2 summary"
        examples.example2.description == "Ex2 description"
        examples.example2.value.p21 == "v1"
        examples.example2.value.p22 == 123
    }

    void "test content examples"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller
class MyController {

    @Operation(description = "method", responses = {
            @ApiResponse(content = @Content(
                examples = {
                    @ExampleObject(
                            name = "example1",
                            summary = "Ex1 summary",
                            description = "Ex1 description",
                            value = "{\\"p1\\": \\"v1\\", \\"p2\\": 123}"
                    ),
                    @ExampleObject(
                            name = "example2",
                            summary = "Ex2 summary",
                            description = "Ex2 description",
                            value = "{\\"p21\\": \\"v1\\", \\"p22\\": 123}"
                    )
                }
            ))
    })
    @Post("/model")
    public String model(@QueryValue String param) {
        return null;
     }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI?.paths?."/model"?.post

        then:
        operation
        def examples = operation.responses.'200'.content.'application/json'.examples
        examples.size() == 2
        examples.example1
        examples.example1.summary == "Ex1 summary"
        examples.example1.description == "Ex1 description"
        examples.example1.value.p1 == "v1"
        examples.example1.value.p2 == 123

        examples.example2
        examples.example2.summary == "Ex2 summary"
        examples.example2.description == "Ex2 description"
        examples.example2.value.p21 == "v1"
        examples.example2.value.p22 == 123
    }

    void "test route parameter, but no matching method argument"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Singleton;

@Controller("/test")
class TestController {

    @Post("{id}")
    HttpResponse<Void> reqPathVar() {
        return HttpResponse.ok();
    }

    @Post("endpoint2{/opt1,opt2}")
    HttpResponse<Void> optPathVars() {
        return HttpResponse.ok();
    }

    @Post("endpoint3{?opt1,opt2}")
    HttpResponse<Void> optQueryParams() {
        return HttpResponse.ok();
    }
}

@Singleton
class MyBean {}
''')
        when: "The OpenAPI is retrieved"
        def openApi = Utils.testReference

        then: "the state is correct"
        openApi != null

        when:
        def reqPathVarOp = openApi.paths.get("/test/{id}").post
        def optPathVarsOp1 = openApi.paths.get("/test/endpoint2/{opt1}").post
        def optPathVarsOp2 = openApi.paths.get("/test/endpoint2/{opt1}/{opt2}").post
        def optQueryParamsOp = openApi.paths.get("/test/endpoint3").post

        then:
        reqPathVarOp
        reqPathVarOp.parameters
        reqPathVarOp.parameters.size() == 1
        reqPathVarOp.parameters[0].in == "path"
        reqPathVarOp.parameters[0].name == "id"
        reqPathVarOp.parameters[0].schema
        reqPathVarOp.parameters[0].schema.type == "string"
        reqPathVarOp.parameters[0].required

        optPathVarsOp1
        optPathVarsOp1.parameters
        optPathVarsOp1.parameters.size() == 1
        optPathVarsOp1.parameters[0].in == "path"
        optPathVarsOp1.parameters[0].name == "opt1"
        optPathVarsOp1.parameters[0].schema
        optPathVarsOp1.parameters[0].schema.type == "string"
        optPathVarsOp1.parameters[0].required

        optPathVarsOp2
        optPathVarsOp2.parameters
        optPathVarsOp2.parameters.size() == 2
        optPathVarsOp2.parameters[0].in == "path"
        optPathVarsOp2.parameters[0].name == "opt1"
        optPathVarsOp2.parameters[0].schema
        optPathVarsOp2.parameters[0].schema.type == "string"
        optPathVarsOp2.parameters[0].required
        optPathVarsOp2.parameters[1].in == "path"
        optPathVarsOp2.parameters[1].name == "opt2"
        optPathVarsOp2.parameters[1].schema
        optPathVarsOp2.parameters[1].schema.type == "string"
        optPathVarsOp2.parameters[1].required

        optQueryParamsOp
        optQueryParamsOp.parameters
        optQueryParamsOp.parameters.size() == 2
        optQueryParamsOp.parameters[0].in == "query"
        optQueryParamsOp.parameters[0].name == "opt1"
        optQueryParamsOp.parameters[0].schema
        optQueryParamsOp.parameters[0].schema.type == "string"
        !optQueryParamsOp.parameters[0].required
        optQueryParamsOp.parameters[1].in == "query"
        optQueryParamsOp.parameters[1].name == "opt2"
        optQueryParamsOp.parameters[1].schema
        optQueryParamsOp.parameters[1].schema.type == "string"
        !optQueryParamsOp.parameters[1].required
    }

    void "test @Body method argument has precedence over other arguments"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Singleton;

import java.util.Optional;

@Serdeable
record SimpleBody(String value) {}

@Serdeable
record FilterProvidedArgument(String id) {}

@Singleton
class TestArgumentBinder implements TypedRequestArgumentBinder<FilterProvidedArgument> {
  @Override
  public Argument<FilterProvidedArgument> argumentType() {
    return Argument.of(FilterProvidedArgument.class);
  }

  @Override
  public BindingResult<FilterProvidedArgument> bind(ArgumentConversionContext<FilterProvidedArgument> context, HttpRequest<?> source) {
    return () -> Optional.of(new FilterProvidedArgument("my-id"));
  }
}

@Controller("/test")
class TestController {
    @Post
    HttpResponse<SimpleBody> save(@Body SimpleBody body, @Parameter(hidden = true) FilterProvidedArgument filterProvidedArgument) {
        return HttpResponse.ok(body);
    }
}

@Singleton
class MyBean {}
''')
        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference

        then: "the state is correct"
        openAPI != null

        when:
        Operation operation = openAPI.paths.get("/test").post

        then:
        operation

        and:
        operation.requestBody
        operation.requestBody.content
        operation.requestBody.content.size() == 1
        operation.requestBody.content."application/json"
        operation.requestBody.content."application/json".schema
        operation.requestBody.content."application/json".schema.$ref == "#/components/schemas/SimpleBody"
    }

    @RestoreSystemProperties
    void "test append micronaut.server.context-path to endpoints"() {
        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_SERVER_CONTEXT_PATH, "/local-path")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, "/server-context-path")

        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Singleton;

@Controller("/test")
class TestController {

    @Get("/save{/id}")
    String save() {
        return null;
    }
}

@Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference
        def paths = openAPI.paths

        then:
        paths
        paths."/server-context-path/local-path/test/save"
        paths."/server-context-path/local-path/test/save".get
        paths."/server-context-path/local-path/test/save/{id}"
        paths."/server-context-path/local-path/test/save/{id}".get
    }

    void "test parameter with incorrect schema type"() {

        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Singleton;

@Controller("/path/to/controller/")
class ControllerExternal {

    @Get(uri = "user/get2", produces = MediaType.APPLICATION_JSON)
    @Operation(
          summary = "This operation is used to test OpenAPI definitions",
          description =
                  "In this we call we fake a request to get a user")
    @ApiResponse(
          responseCode = "200",
          description = "When the request is successful",
          content =
          @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = User.class)))
    @ApiResponse(responseCode = "400", description = "Bad params")
    @ApiResponse(responseCode = "500", description = "Any other exception that cannot be resolved.")
    @Tag(name = "getUser2")
    public HttpResponse<String> getUser2(
          @Parameter(
                  name = "id",
                  description = "Unique identifier",
                  required = true,
                  schema = @Schema(
                          implementation = String.class,
                          type = "query"
                  )
          ) String id) {
        return HttpResponse.ok();
      }

}

class User {
    public String id;
    public String firstName;
    public String lastName;
    public String email;
    @Schema(hidden=true)
    public String address;
    @Schema(hidden=true)
    public String city;
    @Schema(hidden=true)
    public String state;
    @Schema(hidden=true)
    public String zipcode;
    @Schema(hidden=true)
    public String country;
    @Schema(hidden=true)
    public String phoneNumber;
}

class GetUserClientRequest {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

@Singleton
class MyBean {}
''')
        when:
        OpenAPI openAPI = Utils.testReference
        def paths = openAPI.paths

        then:
        paths
        paths."/path/to/controller/user/get2"
        paths."/path/to/controller/user/get2".get
        paths."/path/to/controller/user/get2".get.parameters
        paths."/path/to/controller/user/get2".get.parameters[0]
        paths."/path/to/controller/user/get2".get.parameters[0].name == "id"
        paths."/path/to/controller/user/get2".get.parameters[0].required
        paths."/path/to/controller/user/get2".get.parameters[0].description == "Unique identifier"
        paths."/path/to/controller/user/get2".get.parameters[0].schema
        paths."/path/to/controller/user/get2".get.parameters[0].schema.type == "string"
    }
}
