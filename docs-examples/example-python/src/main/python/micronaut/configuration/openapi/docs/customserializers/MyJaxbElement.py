from typing import Generic, TypeVar

T = TypeVar("T")


# tag::clazz[]
# if you want to use generic from fields with type JAXBElement<T>
class MyJaxbElement(Generic[T]):
    type: str
    value: T
# end::clazz[]
