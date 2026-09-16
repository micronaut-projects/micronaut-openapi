try:
    # tag::imports[]
    from micronaut.runtime import Micronaut
    from io.swagger.v3.oas.annotations import *
    from io.swagger.v3.oas.annotations.info import *
    # end::imports[]
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from swagger.v3.oas.annotations import *
    from swagger.v3.oas.annotations.info import *

try:
    # tag::securityImports[]
    from io.swagger.v3.oas.annotations.enums import SecuritySchemeType
    from io.swagger.v3.oas.annotations.security import *
    # end::securityImports[]
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from swagger.v3.oas.annotations.enums import SecuritySchemeType
    from swagger.v3.oas.annotations.security import *

# tag::excludeImports[]
from micronaut.openapi.annotation import OpenAPIExclude

from .exclude.InternalApi import InternalApi
from .exclude.OldApi import OldApi
# end::excludeImports[]

try:
    # tag::includeImports[]
    from micronaut.management.endpoint.env import EnvironmentEndpoint
    from micronaut.openapi.annotation import OpenAPIInclude
    from micronaut.security.endpoints import LoginController, LogoutController
    from io.swagger.v3.oas.annotations.security import SecurityRequirement
    from io.swagger.v3.oas.annotations.tags import *
    # end::includeImports[]
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from swagger.v3.oas.annotations.security import SecurityRequirement
    from swagger.v3.oas.annotations.tags import *


# tag::securityScheme[]
@SecurityScheme(
    name="openid",
    type=SecuritySchemeType.OAUTH2,
    scheme="bearer",
    bearerFormat="jwt",
    flows=OAuthFlows(
        authorizationCode=OAuthFlow(
            authorizationUrl="https://mycompany.okta.com/oauth2/default/v1/authorize",
            tokenUrl="https://mycompany.okta.com/oauth2/default/v1/token",
            refreshUrl="",
            scopes=OAuthScope(name="openid", description="OpenID role"),
        )
    ),
)
# end::securityScheme[]
# tag::exclude[]
@OpenAPIExclude(
    # you can specify classes to exclude
    classes=[OldApi, InternalApi],
    # or / and you can specify packages to exclude
    packages=[
        "micronaut.configuration.openapi.docs.exclude.package1",
        "micronaut.configuration.openapi.docs.exclude.package2",
    ],
)
# end::exclude[]
# tag::include[]
@OpenAPIInclude(
    # you can specify classes to include
    classes=[LoginController, LogoutController],
    # or / and you can specify packages to include
    packages=[
        "io.different.package.with.controllers",
        "io.different.package.with.controllers2",
    ],
    tags=Tag(name="Security"),
)
@OpenAPIInclude(
    classes=EnvironmentEndpoint,
    tags=Tag(name="Management"),
    security=SecurityRequirement(name="BEARER", scopes=["ADMIN"]),
)
# end::include[]
# tag::clazz[]
@OpenAPIDefinition(
    info=Info(
        title="Hello World",
        version="0.0",
        description="My API",
        license=License(name="Apache 2.0", url="https://foo.bar"),
        contact=Contact(url="https://gigantic-server.com", name="Fred", email="Fred@gigagantic-server.com"),
    )
)
class Application:
    pass


if __name__ == "__main__":
    Micronaut.run(Application)
# end::clazz[]
