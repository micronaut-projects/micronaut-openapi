from dataclasses import dataclass

# tag::imports[]
from micronaut.core.annotation import Introspected
# end::imports[]


# tag::clazz[]
@Introspected
@dataclass
class Person:
    name: str
    debtValue: int
    totalGoals: int
# end::clazz[]
