from dataclasses import dataclass

from micronaut.core.annotation import Introspected

from .BaseCategory import BaseCategory


# tag::clazz[]
@Introspected
@dataclass
class LocalizedCategory(BaseCategory):
    displayName: str = ""
    locale: str = ""
# end::clazz[]
