from dataclasses import dataclass
from typing import Annotated

from micronaut.core.annotation import Introspected

from .PetType import PetType

# tag::imports[]
from io.swagger.v3.oas.annotations.media import Schema
# end::imports[]


# tag::clazz[]
@Schema(name="MyPet", description="Pet description")  # <1>
@Introspected
@dataclass
class Pet:
    type: PetType | None
    age: Annotated[int, Schema(description="Pet age", maximum="20")]  # <2>
    """The age"""
    name: Annotated[str, Schema(description="Pet name", maxLength=20)]
# end::clazz[]
