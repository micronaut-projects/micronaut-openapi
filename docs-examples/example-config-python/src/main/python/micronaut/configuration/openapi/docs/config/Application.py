try:
    # tag::imports[]
    from micronaut.runtime import Micronaut
    from io.swagger.v3.oas.annotations import *
    from io.swagger.v3.oas.annotations.info import *
    # end::imports[]
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from swagger.v3.oas.annotations import *
    from swagger.v3.oas.annotations.info import *


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
