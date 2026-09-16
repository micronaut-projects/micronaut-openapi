from typing import Annotated

# tag::imports[]
from micronaut.http.annotation import Body, Controller, Get, Post
from micronaut.openapi.annotation import OpenAPIGroup
# end::imports[]


# tag::clazz[]
@Controller
class ApiController:

    @OpenAPIGroup(exclude="v2")
    @Get("/read/{id}")
    def read(self, id: str) -> str:
        return "OK!"

    @OpenAPIGroup("v2")
    @Post("/save/{id}")
    def save2(self, id: str, body: Annotated[object, Body]) -> str:
        return "OK!"

    @OpenAPIGroup(["v1", "v2"])
    @Post("/save")
    def save(self, body: Annotated[object, Body]) -> str:
        return "OK!"
# end::clazz[]
