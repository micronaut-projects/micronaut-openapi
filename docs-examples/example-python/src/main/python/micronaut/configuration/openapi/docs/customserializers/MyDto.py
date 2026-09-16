from dataclasses import dataclass

from jakarta.xml.bind import JAXBElement

from .XmlElement import XmlElement
from .XmlElement2 import XmlElement2
from .XmlElement3 import XmlElement3


# tag::clazz[]
@dataclass
class MyDto:
    xmlElement: JAXBElement[XmlElement]
    xmlElement2: JAXBElement[XmlElement2]
    xmlElement3: JAXBElement[XmlElement3]
# end::clazz[]
