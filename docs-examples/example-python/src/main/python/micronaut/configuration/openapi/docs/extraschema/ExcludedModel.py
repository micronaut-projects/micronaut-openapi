from dataclasses import dataclass

from micronaut.core.annotation import Introspected
from micronaut.openapi.annotation import OpenAPIExtraSchema


@OpenAPIExtraSchema
@Introspected
@dataclass
class ExcludedModel:
    field1: str
