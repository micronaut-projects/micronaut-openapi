from typing import Annotated

from micronaut.http.annotation import Body, Controller, Put
from micronaut.configuration.openapi.docs.schemas.Pet import Pet

from .Response import Response


# tag::clazz[]
@Controller
class MyController:

    @Put("/")
    def updatePet(self, pet: Annotated[Pet, Body]) -> Response[Pet]:
        return Response(pet)
# end::clazz[]
