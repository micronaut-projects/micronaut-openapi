package io.micronaut.configuration.openapi.docs.decorator;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;

public interface Api<Req, Resp> {

    @Get("/{id}")
    Resp get(String id);

    @Post
    Resp save(@Body Req request);
}
