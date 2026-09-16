package io.micronaut.configuration.openapi.docs.groups

// tag::imports[]
import io.micronaut.openapi.annotation.OpenAPIGroupInfo
import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
// end::imports[]

// tag::clazz[]
@OpenAPIGroupInfo(
        names = ["v1"],
        info = OpenAPIDefinition(
            info = Info(
                    title = "Public api v1",
                    version = "v1",
                    description = "This is API version 1",
                    license = License(name = "Apache 2.0", url = "https://foo.bar"),
                    contact = Contact(url = "https://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
            )
        )
)
@OpenAPIGroupInfo(
        names = ["v2"],
        info = OpenAPIDefinition(
            info = Info(
                    title = "Public api v2",
                    version = "v2",
                    description = "This is API version 2",
                    license = License(name = "Apache 2.0", url = "https://foo.bar"),
                    contact = Contact(url = "https://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
            )
        )
)
@OpenAPIDefinition(
        info = Info(
                title = "Private api",
                version = "\${service.version}",
                description = "This is API version 2",
                license = License(name = "Apache 2.0", url = "https://foo.bar"),
                contact = Contact(url = "https://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")
        )
)
object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.run(Application.javaClass)
    }
}
// end::clazz[]
