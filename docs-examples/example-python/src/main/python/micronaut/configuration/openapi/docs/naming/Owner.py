from dataclasses import dataclass
from typing import Annotated

from micronaut.core.annotation import Introspected

try:
    # tag::imports[]
    from io.swagger.v3.oas.annotations.media import *
    # end::imports[]
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from swagger.v3.oas.annotations.media import *

from .Pet import Pet


# tag::clazz[]
@Introspected
@dataclass
class Owner:
    bird: Annotated[Pet, Schema(description="Pet that is a bird")]  # <2>
    cat: Annotated[Pet, Schema(description="Pet that is a cat")]  # <3>
    dog: Annotated[Pet, Schema(name="Dog", description="Pet that is a dog")]  # <4>
# end::clazz[]
