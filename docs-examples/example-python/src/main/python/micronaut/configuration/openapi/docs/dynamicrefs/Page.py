from typing import Generic, TypeVar

T = TypeVar("T")


# tag::clazz[]
class Page(Generic[T]):
    items: list[T]
    total: int
# end::clazz[]
