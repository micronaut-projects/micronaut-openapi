package io.micronaut.configuration.openapi.docs.decorator

import io.micronaut.core.annotation.Introspected

@Introspected
class MyRequest {
    var name: String? = null
}
