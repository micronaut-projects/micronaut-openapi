from dataclasses import dataclass

from micronaut.core.annotation import Introspected

# tag::imports[]
from io.swagger.v3.oas.annotations.media import Schema
# end::imports[]


# tag::clazz[]
@Schema(description="Pet")  # <1>
@Introspected
@dataclass
class Pet:
    name: str
# end::clazz[]
