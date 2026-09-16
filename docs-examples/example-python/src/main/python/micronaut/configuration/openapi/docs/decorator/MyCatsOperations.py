# tag::imports[]
from micronaut.http.annotation import Controller
from micronaut.openapi.annotation import OpenAPIDecorator

from .Api import Api
from .MyRequest import MyRequest
from .MyResponse import MyResponse
# end::imports[]


# tag::clazz[]
@OpenAPIDecorator(opIdPrefix="cats-", opIdSuffix="-suffix")
@Controller("/cats")
class MyCatsOperations(Api[MyRequest, MyResponse]):
    pass
# end::clazz[]
