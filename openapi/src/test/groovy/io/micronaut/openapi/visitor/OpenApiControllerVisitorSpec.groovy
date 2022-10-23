package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import spock.lang.Issue

class OpenApiControllerVisitorSpec extends AbstractOpenApiTypeElementSpec {

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
    public HttpResponse<String> publicEnpoint() {
        return null;
    }

    @Hidden
    @Post("/private")
    public HttpResponse<String> internalEnpoint() {
        return null;
    }

    @Operation(hidden = true)
    @Post("/private-operation")
    public HttpResponse<String> internalEnpoint2() {
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
import javax.validation.constraints.*;

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
import javax.validation.constraints.*;

@Controller("/")
class MyController {

    @Get("/subscription/{subscriptionId}")
    public String getSubscription( @Size(min=10, max=20) java.util.List<String> subscriptionId) {
        return null;
     }

    @io.micronaut.http.annotation.Error
    public HttpResponse<JsonError> jsonError(HttpRequest request, JsonParseException jsonParseException) {
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

    void "test parse javax.validation constraints for String"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import javax.validation.constraints.*;

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

    void "test parse javax.validation constraints for String[]"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import javax.validation.constraints.*;

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

    void "test parse javax.validation constraints for List"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import javax.validation.constraints.*;

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

    void "test parse javax.validation.NotEmpty constraint for List"() {
        given:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;
import javax.validation.constraints.*;

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
            @Header String contentType,
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
        operation.parameters[2].name == 'content-type'
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
import javax.validation.constraints.*;

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
        OpenAPI api = Utils.testReferenceAfterPlaceholders

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
        openAPI.components.schemas['TestPojo'].name == 'TestPojo'
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
import io.micronaut.http.MediaType;import io.micronaut.http.annotation.Controller;
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
import io.micronaut.http.MediaType;import io.micronaut.http.annotation.Controller;
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
}
