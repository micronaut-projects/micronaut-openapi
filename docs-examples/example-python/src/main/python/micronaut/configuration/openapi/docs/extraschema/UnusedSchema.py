from dataclasses import dataclass

from micronaut.core.annotation import Introspected

# tag::imports[]
from micronaut.openapi.annotation import OpenAPIExtraSchema
# end::imports[]


# tag::clazz[]
@OpenAPIExtraSchema
@Introspected
@dataclass
class UnusedSchema:
    field1: str
# end::clazz[]
