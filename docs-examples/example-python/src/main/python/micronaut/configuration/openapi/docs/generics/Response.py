from typing import Generic, TypeVar

T = TypeVar("T")


# tag::clazz[]
class Response(Generic[T]):
    result: T

    def __init__(self, result: T):
        self.result = result
# end::clazz[]
