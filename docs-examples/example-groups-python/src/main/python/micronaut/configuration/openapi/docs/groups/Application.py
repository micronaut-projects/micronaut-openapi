# tag::imports[]
from micronaut.openapi.annotation import OpenAPIGroupInfo
from micronaut.runtime import Micronaut
from io.swagger.v3.oas.annotations import OpenAPIDefinition
from io.swagger.v3.oas.annotations.info import Contact, Info, License
# end::imports[]


# tag::clazz[]
@OpenAPIGroupInfo(
    names="v1",
    info=OpenAPIDefinition(
        info=Info(
            title="Public api v1",
            version="v1",
            description="This is API version 1",
            license=License(name="Apache 2.0", url="https://foo.bar"),
            contact=Contact(url="https://gigantic-server.com", name="Fred", email="Fred@gigagantic-server.com"),
        )
    ),
)
@OpenAPIGroupInfo(
    names="v2",
    info=OpenAPIDefinition(
        info=Info(
            title="Public api v2",
            version="v2",
            description="This is API version 2",
            license=License(name="Apache 2.0", url="https://foo.bar"),
            contact=Contact(url="https://gigantic-server.com", name="Fred", email="Fred@gigagantic-server.com"),
        )
    ),
)
@OpenAPIDefinition(
    info=Info(
        title="Private api",
        version="${service.version}",
        description="This is API version 2",
        license=License(name="Apache 2.0", url="https://foo.bar"),
        contact=Contact(url="https://gigantic-server.com", name="Fred", email="Fred@gigagantic-server.com"),
    )
)
class Application:
    pass


if __name__ == "__main__":
    Micronaut.run(Application)
# end::clazz[]
