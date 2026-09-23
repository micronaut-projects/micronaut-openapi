package io.micronaut.configuration.openapi.docs.customserializers;

import jakarta.xml.bind.JAXBElement;

// tag::clazz[]
class MyDto {

    public JAXBElement<? extends XmlElement> xmlElement;
    public JAXBElement<? extends XmlElement2> xmlElement2;
    public JAXBElement<? extends XmlElement3> xmlElement3;
}
// end::clazz[]
