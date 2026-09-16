package io.micronaut.configuration.openapi.docs

import io.swagger.v3.oas.models.security.SecurityScheme
import spock.lang.Specification

class SecuritySchemeSpec extends Specification {

    void "OAuth 2 security scheme"() {
        when:
        def scheme = OpenApiSpec.load().components.securitySchemes["openid"]
        def flow = scheme.flows.authorizationCode

        then:
        scheme.type == SecurityScheme.Type.OAUTH2
        flow.authorizationUrl == "https://mycompany.okta.com/oauth2/default/v1/authorize"
        flow.tokenUrl == "https://mycompany.okta.com/oauth2/default/v1/token"
        flow.scopes["openid"] == "OpenID role"
    }

    void "security requirement on the controller"() {
        when:
        def operation = OpenApiSpec.load().paths["/"].get

        then:
        operation.security[0]["openid"] == ["openid"]
    }
}
