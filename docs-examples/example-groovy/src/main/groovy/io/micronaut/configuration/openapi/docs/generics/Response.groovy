package io.micronaut.configuration.openapi.docs.generics

// tag::clazz[]
class Response<T> {

    private T r

    T getResult() {
        return r
    }

    void setResult(T r) {
        this.r = r
    }
}
// end::clazz[]
