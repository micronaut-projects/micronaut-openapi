package io.micronaut.configuration.openapi.docs;

import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecuritySchemeTest {

    @Test
    void oauth2SecurityScheme() {
        var openApi = OpenApiSpec.load();

        var scheme = openApi.getComponents().getSecuritySchemes().get("openid");
        assertEquals(SecurityScheme.Type.OAUTH2, scheme.getType());
        var flow = scheme.getFlows().getAuthorizationCode();
        assertEquals("https://mycompany.okta.com/oauth2/default/v1/authorize", flow.getAuthorizationUrl());
        assertEquals("https://mycompany.okta.com/oauth2/default/v1/token", flow.getTokenUrl());
        assertEquals("OpenID role", flow.getScopes().get("openid"));
    }

    @Test
    void securityRequirementOnTheController() {
        var operation = OpenApiSpec.load().getPaths().get("/").getGet();

        assertEquals(List.of("openid"), operation.getSecurity().get(0).get("openid"));
    }
}
