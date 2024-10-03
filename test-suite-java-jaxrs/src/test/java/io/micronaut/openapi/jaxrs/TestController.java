package io.micronaut.openapi.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/user")
class TestController {

    @GET
    @Path("/{userId}")
    @Produces("application/json")
    public String getUser(@PathParam("userId") String userId) {
        return "Pong version " + userId;
    }

}
