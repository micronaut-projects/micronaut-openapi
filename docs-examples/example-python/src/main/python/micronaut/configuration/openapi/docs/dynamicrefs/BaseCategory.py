from dataclasses import dataclass, field

from micronaut.core.annotation import Introspected


# tag::clazz[]
@Introspected
@dataclass
class BaseCategory:
    children: list["BaseCategory"] = field(default_factory=list)
# end::clazz[]
