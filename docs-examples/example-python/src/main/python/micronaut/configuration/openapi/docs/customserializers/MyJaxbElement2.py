from dataclasses import dataclass

from micronaut.core.annotation import Introspected


# tag::clazz[]
@Introspected
@dataclass
class MyJaxbElement2:
    type: str
    values: list[str]
# end::clazz[]
