package io.micronaut.configuration.openapi.docs.customserializers

// tag::clazz[]
// if you want to use generic from fields with type JAXBElement<T>
class MyJaxbElement<T> {
    var type: String? = null
    var value: T? = null
}
// end::clazz[]
