package io.micronaut.configuration.openapi.docs;
// tag::imports[]
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
// end::imports[]
// tag::securityImports[]
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
// end::securityImports[]
// tag::excludeImports[]
import io.micronaut.configuration.openapi.docs.exclude.InternalApi;
import io.micronaut.configuration.openapi.docs.exclude.OldApi;
import io.micronaut.openapi.annotation.OpenAPIExclude;
// end::excludeImports[]
// tag::includeImports[]
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
// end::includeImports[]

// tag::securityScheme[]
@SecurityScheme(name = "openid",
        type = SecuritySchemeType.OAUTH2,
        scheme = "bearer",
        bearerFormat = "jwt",
        flows = @OAuthFlows(
                authorizationCode = @OAuthFlow(
                        authorizationUrl = "https://mycompany.okta.com/oauth2/default/v1/authorize",
                        tokenUrl = "https://mycompany.okta.com/oauth2/default/v1/token",
                        refreshUrl = "",
                        scopes = @OAuthScope(name = "openid", description = "OpenID role")
                )
        )
)
// end::securityScheme[]
// tag::exclude[]
@OpenAPIExclude(
        // you can specify classes to exclude
        classes = {
                OldApi.class,
                InternalApi.class,
        },
        // or / and you can specify packages to exclude
        packages = {
            "io.exclude.package.with.controllers",
            "io.exclude.package.with.controllers2",
        }
)
// end::exclude[]
// tag::include[]
@OpenAPIInclude(
        // you can specify classes to include
        classes = {
                io.micronaut.security.endpoints.LoginController.class,
                io.micronaut.security.endpoints.LogoutController.class
        },
        // or / and you can specify packages to include
        packages = {
            "io.different.package.with.controllers",
            "io.different.package.with.controllers2",
        },
        tags = @Tag(name = "Security")
)
@OpenAPIInclude(
        classes = io.micronaut.management.endpoint.env.EnvironmentEndpoint.class,
        tags = @Tag(name = "Management"),
        security = @SecurityRequirement(name = "BEARER", scopes = {"ADMIN"})
)
// end::include[]
// tag::clazz[]
@OpenAPIDefinition(
        info = @Info(
                title = "Hello World",
                version = "0.0",
                description = "My API",
                license = @License(name = "Apache 2.0", url = "https://foo.bar"),
                contact = @Contact(url = "https://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
        )
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class);
    }
}
//end::clazz[]
