from micronaut.http.annotation import Controller
from micronaut.openapi.annotation import OpenAPIDecorator

from .Api import Api
from .MyRequest import MyRequest
from .MyResponse import MyResponse


# tag::clazz[]
@OpenAPIDecorator("dogs-")
@Controller("/dogs")
class MyDogsOperations(Api[MyRequest, MyResponse]):
    pass
# end::clazz[]
