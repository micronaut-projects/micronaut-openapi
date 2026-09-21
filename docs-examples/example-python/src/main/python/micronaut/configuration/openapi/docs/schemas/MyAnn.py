# tag::imports[]
from io.swagger.v3.oas.annotations.media import Schema
# end::imports[]


# tag::clazz[]
@Schema(name="MyPet", description="Pet description")
def MyAnn(target):
    return target
# end::clazz[]
