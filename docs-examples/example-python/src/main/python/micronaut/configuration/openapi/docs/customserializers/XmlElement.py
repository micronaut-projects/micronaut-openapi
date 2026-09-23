from dataclasses import dataclass

from micronaut.core.annotation import Introspected


# tag::clazz[]
@Introspected
@dataclass
class XmlElement:
    propStr: str
# end::clazz[]
