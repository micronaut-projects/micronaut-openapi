package io.micronaut.configuration.openapi.docs
// tag::imports[]
import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License

// end::imports[]
// tag::clazz[]
@OpenAPIDefinition(
        info = Info(
                title = "Hello World",
                version = "0.0",
                description = "My API",
                license = License(name = "Apache 2.0", url = "http://foo.bar"),
                contact = Contact(url = "http://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
        )
)
object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.run(Application.javaClass)
    }
}
// end::clazz[]