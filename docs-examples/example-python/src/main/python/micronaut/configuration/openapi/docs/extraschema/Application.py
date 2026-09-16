# tag::imports[]
from micronaut.openapi.annotation import OpenAPIExtraSchema

from .ExcludedModel import ExcludedModel
from .UnusedModel1 import UnusedModel1
# end::imports[]


# tag::clazz[]
@OpenAPIExtraSchema(
    # classes to add
    classes=UnusedModel1,
    # excluded classes, which marked with `@OpenAPIExtraSchema` annotation
    excludeClasses=ExcludedModel,
    # exclude classes by packages
    excludePackages="micronaut.configuration.openapi.docs.extraschema.exclude",
    # include classes by packages
    packages="micronaut.configuration.openapi.docs.extraschema.extra",
)
class Application:
    pass
# end::clazz[]
