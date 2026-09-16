package io.micronaut.configuration.openapi.docs.config

import spock.lang.Specification

class PropertiesConfigurationSpec extends Specification {

    void "placeholders are expanded"() {
        when:
        def info = OpenApiSpec.load("hello-world-v1.1.yml").info

        then:
        info.title == "Hello World"
        info.description == "A nice API"
        info.version == "v1.1"
        info.contact.name == "Fred"
        info.license.name == "Apache 2.0"
    }
}
