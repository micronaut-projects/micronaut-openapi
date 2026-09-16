try:
    # tag::imports[]
    from micronaut.http.annotation import Controller, Get
    from micronaut.security.annotation import Secured
    from micronaut.security.rules import SecurityRule
    from io.swagger.v3.oas.annotations.security import *
    # end::imports[]
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from swagger.v3.oas.annotations.security import *


# tag::clazz[]
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
class OrderController:

    @Get
    @SecurityRequirement(name="openid", scopes="openid")
    def index(self) -> str:
        return "Example Response"
# end::clazz[]
