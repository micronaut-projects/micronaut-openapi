package io.micronaut.configuration.openapi.docs.security;

// tag::imports[]
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
// end::imports[]

// tag::clazz[]
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
public class OrderController {

    @Get
    @SecurityRequirement(name = "openid", scopes = "openid")
    public String index() {
        return "Example Response";
    }
}
// end::clazz[]
