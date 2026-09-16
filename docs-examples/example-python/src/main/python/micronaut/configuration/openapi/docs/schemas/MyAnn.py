try:
    # tag::imports[]
    from io.swagger.v3.oas.annotations.media import *
    # end::imports[]
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from swagger.v3.oas.annotations.media import *


# tag::clazz[]
@Schema(name="MyPet", description="Pet description")
def MyAnn(target):
    return target
# end::clazz[]
