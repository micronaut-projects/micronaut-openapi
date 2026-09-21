from dataclasses import dataclass
from typing import Annotated

from micronaut.core.annotation import Introspected

# tag::imports[]
from io.swagger.v3.oas.annotations.media import Schema
# end::imports[]

from .Pet import Pet


# tag::clazz[]
@Introspected
@dataclass
class Owner:
    cat: Pet  # <2>
    dog: Annotated[Pet, Schema(name="MyPet", description="This is my pet")]  # <3>
# end::clazz[]
