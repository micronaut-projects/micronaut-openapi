from dataclasses import dataclass
from typing import Annotated

# tag::imports[]
from jakarta.validation.constraints import NotNull
from micronaut.core.annotation import Introspected
# end::imports[]


# tag::clazz[]
@Introspected
@dataclass
class UserDto:
    name: str | None = None
    age: int = 0
    secondName: str | None = None
    address: Annotated[str, NotNull] = None
# end::clazz[]
