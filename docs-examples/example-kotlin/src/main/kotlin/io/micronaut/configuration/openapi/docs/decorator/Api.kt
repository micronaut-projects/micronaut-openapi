package io.micronaut.configuration.openapi.docs.decorator

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

interface Api<Req, Resp> {

    @Get("/{id}")
    fun get(id: String): Resp

    @Post
    fun save(@Body request: Req): Resp
}
