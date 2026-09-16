package io.micronaut.configuration.openapi.docs.dynamicrefs

// tag::clazz[]
class LocalizedCategory : BaseCategory() {
    var displayName: String? = null
    var locale: String? = null
}
// end::clazz[]
