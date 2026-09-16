from dataclasses import dataclass

from micronaut.core.annotation import Introspected


@Introspected
@dataclass
class UnusedModel1:
    field1: str
