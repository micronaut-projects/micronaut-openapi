from typing import Annotated, Generic, TypeVar

from micronaut.http.annotation import Body, Get, Post

Req = TypeVar("Req")
Resp = TypeVar("Resp")


class Api(Generic[Req, Resp]):

    @Get("/{id}")
    def get(self, id: str) -> Resp:
        ...

    @Post
    def save(self, request: Annotated[Req, Body]) -> Resp:
        ...
