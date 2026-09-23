from dataclasses import dataclass
from typing import Generic, TypeVar

from micronaut.core.annotation import Introspected

T = TypeVar("T")


# tag::clazz[]
@Introspected
@dataclass
class Page(Generic[T]):
    items: list[T]
    total: int
# end::clazz[]
