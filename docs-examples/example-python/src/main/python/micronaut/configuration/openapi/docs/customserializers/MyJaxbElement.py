from dataclasses import dataclass
from typing import Generic, TypeVar

from micronaut.core.annotation import Introspected

T = TypeVar("T")


# tag::clazz[]
# if you want to use generic from fields with type JAXBElement<T>
@Introspected
@dataclass
class MyJaxbElement(Generic[T]):
    type: str
    value: T
# end::clazz[]
