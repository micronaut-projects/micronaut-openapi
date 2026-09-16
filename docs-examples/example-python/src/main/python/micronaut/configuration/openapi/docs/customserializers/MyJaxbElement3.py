from dataclasses import dataclass

from micronaut.core.annotation import Introspected


# tag::clazz[]
@Introspected
@dataclass
class MyJaxbElement3:
    type: str
    value: str
# end::clazz[]
