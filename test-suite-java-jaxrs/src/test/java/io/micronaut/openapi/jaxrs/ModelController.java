package io.micronaut.openapi.jaxrs;

import io.micronaut.openapi.jaxrs.model.one.Response;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/model")
class ModelController {
    @GET
    @Path("/model-one")
    @Produces("application/json")
    public Response getResponseOne() {
        return new Response("one");
    }

    @GET
    @Path("/model-one-b")
    @Produces("application/json")
    public Response getResponseOneB() {
        return new Response("one-b");
    }

    @GET
    @Path("/model-two")
    @Produces("application/json")
    public io.micronaut.openapi.jaxrs.model.two.Response getResponseTwo() {
        return new io.micronaut.openapi.jaxrs.model.two.Response("two");
    }

    @GET
    @Path("/model-two-b")
    @Produces("application/json")
    public io.micronaut.openapi.jaxrs.model.two.Response getResponseTwoB() {
        return new io.micronaut.openapi.jaxrs.model.two.Response("two-b");
    }

    @GET
    @Path("/model-two-c")
    @Produces("application/json")
    public io.micronaut.openapi.jaxrs.model.two.Response getResponseTwoC() {
        return new io.micronaut.openapi.jaxrs.model.two.Response("two-c");
    }

}
