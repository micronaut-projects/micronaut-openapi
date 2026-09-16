package io.micronaut.configuration.openapi.docs.decorator;

// tag::imports[]
import io.micronaut.http.annotation.Controller;
import io.micronaut.openapi.annotation.OpenAPIDecorator;
// end::imports[]

// tag::clazz[]
@OpenAPIDecorator(opIdPrefix = "cats-", opIdSuffix = "-suffix")
@Controller("/cats")
interface MyCatsOperations extends Api<MyRequest, MyResponse> {
}
// end::clazz[]
