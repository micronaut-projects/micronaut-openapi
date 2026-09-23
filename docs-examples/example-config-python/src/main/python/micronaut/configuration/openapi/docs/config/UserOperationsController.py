from typing import Annotated

# tag::imports[]
from micronaut.http.annotation import Body, Controller, Get, Post
# end::imports[]


# tag::clazz[]
@Controller
class UserOperationsController:
    """User main operations."""

    @Get("/read/{id}")
    def read(self, id: str) -> str:
        return "OK!"

    @Post("/save/{id}")
    def save2(self, id: str, body: Annotated[object, Body]) -> str:
        return "OK!"

    @Post("/save")
    def save(self, body: Annotated[object, Body]) -> str:
        return "OK!"
# end::clazz[]
