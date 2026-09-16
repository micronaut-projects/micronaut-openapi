package io.micronaut.configuration.openapi.docs

import io.swagger.v3.oas.models.security.SecurityScheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SecuritySchemeTest {

    @Test
    fun oauth2SecurityScheme() {
        val openApi = OpenApiSpec.load()

        val scheme = openApi.components.securitySchemes["openid"]!!
        assertEquals(SecurityScheme.Type.OAUTH2, scheme.type)
        val flow = scheme.flows.authorizationCode
        assertEquals("https://mycompany.okta.com/oauth2/default/v1/authorize", flow.authorizationUrl)
        assertEquals("https://mycompany.okta.com/oauth2/default/v1/token", flow.tokenUrl)
        assertEquals("OpenID role", flow.scopes["openid"])
    }

    @Test
    fun securityRequirementOnTheController() {
        val operation = OpenApiSpec.load().paths["/"]!!.get

        assertEquals(listOf("openid"), operation.security[0]["openid"])
    }
}
