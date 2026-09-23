# tag::imports[]
from micronaut.core.version.annotation import Version
from micronaut.http.annotation import Controller, Get, Post
# end::imports[]

from .UserDto import UserDto


# tag::clazz[]
@Controller("/versioned")
class VersionedController:

    @Version("1")
    @Get("/hello")
    def helloV1(self) -> str:
        return "helloV1"

    @Version("2")
    @Post("/hello")
    def helloV2(self, userDto: UserDto) -> str:
        return "helloV2"

    @Post("/common")
    def common(self) -> str:
        return None
# end::clazz[]
