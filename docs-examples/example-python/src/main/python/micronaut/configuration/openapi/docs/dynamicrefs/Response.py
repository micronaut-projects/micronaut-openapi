from typing import Generic, TypeVar

T = TypeVar("T")


# tag::clazz[]
class Response(Generic[T]):
    data: T
# end::clazz[]
