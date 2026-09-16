from dataclasses import dataclass

from micronaut.core.annotation import Introspected


# tag::clazz[]
@Introspected
@dataclass
class XmlElement3:
    propStr3: str
# end::clazz[]
