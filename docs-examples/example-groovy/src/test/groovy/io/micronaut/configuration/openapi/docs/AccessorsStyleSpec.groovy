package io.micronaut.configuration.openapi.docs

import spock.lang.Specification

class AccessorsStyleSpec extends Specification {

    void "custom accessors are detected"() {
        when:
        def person = OpenApiSpec.load().components.schemas["Person"]

        then:
        person.properties.keySet() as List == ["name", "debtValue", "totalGoals"]
        person.properties["name"].type == "string"
        person.properties["debtValue"].type == "integer"
        person.properties["totalGoals"].type == "integer"
    }
}
