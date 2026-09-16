from typing import Annotated

from micronaut.http.annotation import Body, Controller, Get, Post

from .MyAnn import MyAnn
from .Pet import Pet
from .PetType import PetType


@Controller("/pets")
class PetController:

    @Get("/{name}")
    def getPet(self, name: str) -> Pet:
        return Pet(PetType.DOG, 1, name)

    @Post
    def savePet(self, pet: Annotated[Pet, Body, MyAnn]) -> Pet:
        return pet
