package io.micronaut.configuration.openapi.docs

import spock.lang.Specification

class OpenApiExcludeSpec extends Specification {

    void "excluded controllers are not documented"() {
        when:
        def paths = OpenApiSpec.load().paths

        then:
        !paths.containsKey("/old")
        !paths.containsKey("/internal")
    }
}
