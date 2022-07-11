package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiOperationCallbackSpec extends AbstractOpenApiTypeElementSpec {

    void "test parse the OpenAPI @Operation annotation with @Callback"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.callbacks.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Post("/test")
    @Callback(
            callbackUrlExpression = "https://$request.query.url",
            name = "subscription",
            operation = {
                    @Operation(
                            method = "post",
                            description = "payload data will be sent",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "subscriptionId", required = true, schema = @Schema(
                                            type = "string",
                                            format = "uuid",
                                            description = "the generated UUID",
                                            accessMode = Schema.AccessMode.READ_ONLY
                                    ))
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Return this code if the callback was received and processed successfully"
                                    ),
                                    @ApiResponse(
                                            responseCode = "205",
                                            description = "Return this code to unsubscribe from future data updates"
                                    ),
                                    @ApiResponse(
                                            responseCode = "default",
                                            description = "All other response codes will disable this callback subscription"
                                    )
                            }),
                    @Operation(
                            method = "get",
                            description = "payload data will be received"
                    ),
                    @Operation(
                            method = "put",
                            description = "payload data will be sent"
                    )})
    @Operation(description = "subscribes a client to updates relevant to the requestor's account, as " +
            "identified by the input token.  The supplied url will be used as the delivery address for response payloads")
    public SubscriptionResponse subscribe(@Schema(required = true, description = "the authentication token " +
            "provided after initially authenticating to the application") @Header("x-auth-token") String token,
                                          @Schema(required = true, description = "the URL to call with response " +
                                                  "data") @QueryValue("url") String url) {
        return null;
    }

}
class SubscriptionResponse {
        private String subscriptionUuid;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/test")?.post

        expect:
        operation
        operation.description.startsWith('subscribes')
        operation.parameters.size() == 2
        operation.parameters[0].name == 'x-auth-token'
        operation.parameters[0].in == 'header'
        operation.parameters[0].required
        operation.parameters[1].name == 'url'
        operation.parameters[1].in == 'query'
        operation.parameters[1].required
        operation.parameters[1].schema.description.contains("the URL")
        operation.parameters[0].schema.description.contains("the authentication token")
        operation.callbacks
        operation.callbacks['subscription']
        operation.callbacks['subscription']['https://$request.query.url']
        operation.callbacks['subscription']['https://$request.query.url'].get
        operation.callbacks['subscription']['https://$request.query.url'].post
        operation.callbacks['subscription']['https://$request.query.url'].put
        operation.callbacks['subscription']['https://$request.query.url'].post.description == 'payload data will be sent'
        operation.callbacks['subscription']['https://$request.query.url'].post.parameters.size() == 1
        operation.callbacks['subscription']['https://$request.query.url'].post.parameters[0].name == 'subscriptionId'
        operation.callbacks['subscription']['https://$request.query.url'].post.parameters[0].schema.description == 'the generated UUID'
        operation.callbacks['subscription']['https://$request.query.url'].post.parameters[0].schema.format == 'uuid'
//        operation.callbacks['subscription']['https://$request.query.url'].post.parameters[0].schema.readOnly

    }

    void "test OpenAPI @Callback with ref"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.callbacks.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/")
class MyController {

    @Post("/test")
    @Callback(
            callbackUrlExpression = "https://$request.query.url",
            name = "subscription",
            ref = "#/components/callbacks/SomethingElse",
            operation = {
                    @Operation(
                            method = "post",
                            description = "payload data will be sent",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "subscriptionId", required = true, schema = @Schema(
                                            type = "string",
                                            format = "uuid",
                                            description = "the generated UUID",
                                            accessMode = Schema.AccessMode.READ_ONLY
                                    ))
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Return this code if the callback was received and processed successfully"
                                    ),
                                    @ApiResponse(
                                            responseCode = "205",
                                            description = "Return this code to unsubscribe from future data updates"
                                    ),
                                    @ApiResponse(
                                            responseCode = "default",
                                            description = "All other response codes will disable this callback subscription"
                                    )
                            }),
                    @Operation(
                            method = "get",
                            description = "payload data will be received"
                    ),
                    @Operation(
                            method = "put",
                            description = "payload data will be sent"
                    )})
    @Operation(description = "subscribes a client to updates relevant to the requestor's account, as " +
            "identified by the input token.  The supplied url will be used as the delivery address for response payloads")
    public SubscriptionResponse subscribe(@Schema(required = true, description = "the authentication token " +
            "provided after initially authenticating to the application") @Header("x-auth-token") String token,
                                          @Schema(required = true, description = "the URL to call with response " +
                                                  "data") @QueryValue("url") String url) {
        return null;
    }

}
class SubscriptionResponse {
        private String subscriptionUuid;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/test")?.post

        expect:
        operation
        operation.description.startsWith('subscribes')
        operation.parameters.size() == 2
        operation.parameters[0].name == 'x-auth-token'
        operation.parameters[0].in == 'header'
        operation.parameters[0].required
        operation.parameters[1].name == 'url'
        operation.parameters[1].in == 'query'
        operation.parameters[1].required
        operation.parameters[1].schema.description.contains("the URL")
        operation.parameters[0].schema.description.contains("the authentication token")
        operation.callbacks
        operation.callbacks['subscription'] != null
        operation.callbacks['subscription'].get$ref()
        operation.callbacks['subscription'].get$ref() == "#/components/callbacks/SomethingElse"
    }

    void "test OpenAPI @Callbacks with ref"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.callbacks.Callback;
import io.swagger.v3.oas.annotations.callbacks.Callbacks;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller("/")
class MyController {

    @Post("/test")
    @Callbacks({
            @Callback(
                name = "subscription1",
                ref = "#/components/callbacks/Sub1"
            ),
            @Callback(
                name = "subscription2",
                ref = "#/components/callbacks/Sub2"
            ),
            @Callback(
                name = "subscription3",
                ref = "#/components/callbacks/Sub3"
            ),
    })
    @Operation(description = "subscribes a client to updates relevant to the requestor's account, as " +
            "identified by the input token.  The supplied url will be used as the delivery address for response payloads")
    public SubscriptionResponse subscribe(@Schema(required = true, description = "the authentication token " +
            "provided after initially authenticating to the application") @Header("x-auth-token") String token,
                                          @Schema(required = true, description = "the URL to call with response " +
                                                  "data") @QueryValue("url") String url) {
        return null;
    }

}
class SubscriptionResponse {
        private String subscriptionUuid;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths?.get("/test")?.post

        expect:
        operation
        operation.description.startsWith('subscribes')
        operation.parameters.size() == 2
        operation.parameters[0].name == 'x-auth-token'
        operation.parameters[0].in == 'header'
        operation.parameters[0].required
        operation.parameters[1].name == 'url'
        operation.parameters[1].in == 'query'
        operation.parameters[1].required
        operation.parameters[1].schema.description.contains("the URL")
        operation.parameters[0].schema.description.contains("the authentication token")
        operation.callbacks
        operation.callbacks['subscription1'] != null
        operation.callbacks['subscription1'].get$ref()
        operation.callbacks['subscription1'].get$ref() == "#/components/callbacks/Sub1"
        operation.callbacks['subscription2'] != null
        operation.callbacks['subscription2'].get$ref()
        operation.callbacks['subscription2'].get$ref() == "#/components/callbacks/Sub2"
        operation.callbacks['subscription3'] != null
        operation.callbacks['subscription3'].get$ref()
        operation.callbacks['subscription3'].get$ref() == "#/components/callbacks/Sub3"
    }
}
