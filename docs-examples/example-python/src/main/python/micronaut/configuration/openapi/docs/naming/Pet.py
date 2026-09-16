from dataclasses import dataclass

from micronaut.core.annotation import Introspected

try:
    # tag::imports[]
    from io.swagger.v3.oas.annotations.media import *
    # end::imports[]
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from swagger.v3.oas.annotations.media import *


# tag::clazz[]
@Schema(description="A pet")  # <1>
@Introspected
@dataclass
class Pet:
    name: str
# end::clazz[]
