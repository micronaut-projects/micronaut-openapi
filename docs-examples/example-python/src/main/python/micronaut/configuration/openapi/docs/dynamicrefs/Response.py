from dataclasses import dataclass
from typing import Generic, TypeVar

from micronaut.core.annotation import Introspected

T = TypeVar("T")


# tag::clazz[]
@Introspected
@dataclass
class Response(Generic[T]):
    data: T
# end::clazz[]
