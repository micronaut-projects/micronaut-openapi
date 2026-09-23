from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from . import OpenApiSpec

from io.swagger.v3.oas.models.security.SecurityScheme import Type as SecuritySchemeType


@MicronautTest
class SecuritySchemeTest:

    @Test
    def test_oauth2_security_scheme(self):
        open_api = OpenApiSpec.load()

        scheme = open_api.getComponents().getSecuritySchemes().get("openid")
        assert scheme.getType() == SecuritySchemeType.OAUTH2
        flow = scheme.getFlows().getAuthorizationCode()
        assert flow.getAuthorizationUrl() == "https://mycompany.okta.com/oauth2/default/v1/authorize"
        assert flow.getTokenUrl() == "https://mycompany.okta.com/oauth2/default/v1/token"
        assert flow.getScopes().get("openid") == "OpenID role"

    @Test
    def test_security_requirement_on_the_controller(self):
        operation = OpenApiSpec.load().getPaths().get("/").getGet()

        assert list(operation.getSecurity().get(0).get("openid")) == ["openid"]
