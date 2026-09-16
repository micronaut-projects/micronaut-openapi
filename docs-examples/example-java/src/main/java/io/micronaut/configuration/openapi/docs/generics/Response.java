package io.micronaut.configuration.openapi.docs.generics;

// tag::clazz[]
class Response<T> {

    private T r;

    public T getResult() {
        return r;
    }

    public void setResult(T r) {
        this.r = r;
    }
}
// end::clazz[]
