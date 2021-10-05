package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import spock.lang.Issue

class OpenApiReturnVoidSpec extends AbstractOpenApiTypeElementSpec {

    @Issue("https://github.com/micronaut-projects/micronaut-openapi/issues/570")
    void "test returning Void with different reactive types"() {
        given:
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.reactivex.*;
import reactor.core.publisher.*;

@Controller
class ReactiveController {

    @Get("/monoVoid")
    Mono<Void> monoVoid() {
        return Mono.empty();
    }

    @Get("/fluxVoid")
    Flux<Void> fluxVoid() {
        return Flux.empty();
    }

    @Get("/maybeVoid")
    Maybe<Void> maybeVoid() {
        return Maybe.empty();
    }

    @Get("/flowableVoid")
    Flowable<Void> flowableVoid() {
        return Flowable.empty();
    }

    @Get("/void")
    void returnVoid() {
    }

    @Get("/voidType")
    Void returnVoidType() {
        return null;
    }

    @Get("/singleMessage")
    Single<Message> singleMessage() {
        return Single.just(new Message("msg"));
    }

    @Get("/monoMessage")
    Mono<Message> monoMessage() {
        return Mono.just(new Message("msg"));
    }

    @Get("/singleHttpResponse")
    Single<HttpResponse<Message>> singleHttpResponse() {
        return Single.just(HttpResponse.ok(new Message("msg")));
    }

    @Get("/monoHttpResponse")
    Mono<HttpResponse<Message>> monoHttpResponse() {
        return Mono.just(HttpResponse.ok(new Message("msg")));
    }
}

class Message {
    private final String text;

    public Message(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        AbstractOpenApiVisitor.testReference != null

        when:
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference

        then:
        openAPI.components.schemas.size() == 1
        openAPI.components.schemas['Message']

        and: 'return type Void (either direct or wrapped) is handled correctly without creating an "artificial" Void schema'
        !openAPI.paths['/monoVoid'].get.responses['200'].content
        !openAPI.paths['/fluxVoid'].get.responses['200'].content
        !openAPI.paths['/maybeVoid'].get.responses['200'].content
        !openAPI.paths['/flowableVoid'].get.responses['200'].content
        !openAPI.paths['/void'].get.responses['200'].content
        !openAPI.paths['/voidType'].get.responses['200'].content

        and: 'wrapped types that are not Void include the appropriate schema in the response'
        openAPI.paths['/singleHttpResponse'].get.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/Message'
        openAPI.paths['/monoHttpResponse'].get.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/Message'
        openAPI.paths['/singleMessage'].get.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/Message'
        openAPI.paths['/monoMessage'].get.responses['200'].content['application/json'].schema.$ref == '#/components/schemas/Message'
    }

}
