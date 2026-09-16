from dataclasses import dataclass
from typing import Annotated

from micronaut.core.annotation import Introspected

from .PetType import PetType

try:
    # tag::imports[]
    from io.swagger.v3.oas.annotations.media import *
    # end::imports[]
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from swagger.v3.oas.annotations.media import *


# tag::clazz[]
@Schema(name="MyPet", description="Pet description")  # <1>
@Introspected
@dataclass
class Pet:
    type: PetType
    age: Annotated[int, Schema(description="Pet age", maximum="20")]  # <2>
    """The age"""
    name: Annotated[str, Schema(description="Pet name", maxLength=20)]
# end::clazz[]
