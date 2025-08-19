package io.micronaut.openapi.test

import io.micronaut.runtime.Micronaut

fun main(args: Array<String>) {
    Micronaut.build(*args)
        .defaultEnvironments("local")
        .banner(false)
        .start()
}
