from dataclasses import dataclass

from micronaut.core.annotation import Introspected


# tag::clazz[]
@Introspected
@dataclass
class XmlElement2:
    propStr2: str
# end::clazz[]
