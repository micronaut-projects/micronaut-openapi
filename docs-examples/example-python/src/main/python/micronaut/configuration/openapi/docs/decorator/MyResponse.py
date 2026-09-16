from dataclasses import dataclass

from micronaut.core.annotation import Introspected


@Introspected
@dataclass
class MyResponse:
    name: str
