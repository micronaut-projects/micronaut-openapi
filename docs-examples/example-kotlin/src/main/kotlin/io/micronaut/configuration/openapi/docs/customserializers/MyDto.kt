package io.micronaut.configuration.openapi.docs.customserializers

import jakarta.xml.bind.JAXBElement

// tag::clazz[]
class MyDto {

    var xmlElement: JAXBElement<out XmlElement>? = null
    var xmlElement2: JAXBElement<out XmlElement2>? = null
    var xmlElement3: JAXBElement<out XmlElement3>? = null
}
// end::clazz[]
