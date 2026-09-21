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
    bird: Annotated[Pet, Schema(description="Pet that is a bird")]  # <2>
    cat: Annotated[Pet, Schema(description="Pet that is a cat")]  # <3>
    dog: Annotated[Pet, Schema(name="Dog", description="Pet that is a dog")]  # <4>
# end::clazz[]
