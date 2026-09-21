# tag::imports[]
from micronaut.runtime import Micronaut
from io.swagger.v3.oas.annotations import OpenAPIDefinition
from io.swagger.v3.oas.annotations.info import Contact, Info, License
# end::imports[]


# tag::clazz[]
@OpenAPIDefinition(
    info=Info(
        title="${my.api.title}",
        version="${api.version}",
        description="${openapi.description}",
        license=License(name="Apache 2.0", url="https://foo.bar"),
        contact=Contact(url="https://gigantic-server.com", name="Fred", email="Fred@gigagantic-server.com"),
    )
)
class Application:
    pass


if __name__ == "__main__":
    Micronaut.run(Application)
# end::clazz[]
