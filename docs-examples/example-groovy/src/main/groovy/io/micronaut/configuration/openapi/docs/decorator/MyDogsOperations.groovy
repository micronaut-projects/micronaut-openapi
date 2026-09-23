package io.micronaut.configuration.openapi.docs.decorator

import io.micronaut.http.annotation.Controller
import io.micronaut.openapi.annotation.OpenAPIDecorator

// tag::clazz[]
@OpenAPIDecorator("dogs-")
@Controller("/dogs")
interface MyDogsOperations extends Api<MyRequest, MyResponse> {
}
// end::clazz[]
