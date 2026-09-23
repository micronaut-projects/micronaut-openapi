from typing import Annotated

from micronaut.http.annotation import Body, Controller, Put
from micronaut.configuration.openapi.docs.schemas.Pet import Pet

# tag::imports[]
from io.swagger.v3.oas.annotations.media import Schema
# end::imports[]

from .Response import Response


@Controller("/named")
class NamedResponseController:

    # tag::method[]
    @Put("/")
    @Schema(name="ResponseOfPet")
    def updatePet(self, pet: Annotated[Pet, Body]) -> Response[Pet]:
        return Response(pet)
    # end::method[]
