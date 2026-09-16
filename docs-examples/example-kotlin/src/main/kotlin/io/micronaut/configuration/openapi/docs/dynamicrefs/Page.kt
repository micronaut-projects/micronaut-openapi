package io.micronaut.configuration.openapi.docs.dynamicrefs

// tag::clazz[]
class Page<T> {
    var items: List<T>? = null
    var total: Int = 0
}
// end::clazz[]
