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
    cat: Pet  # <2>
    dog: Annotated[Pet, Schema(name="MyPet", description="This is my pet")]  # <3>
# end::clazz[]
