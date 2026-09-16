package io.micronaut.configuration.openapi.docs

import spock.lang.Specification

class OpenApiIncludeSpec extends Specification {

    void "compiled controllers are included"() {
        when:
        def paths = OpenApiSpec.load().paths
        def login = paths["/login"].post
        def env = paths["/env"].get

        then:
        login.tags == ["Security"]
        paths["/logout"]
        env.tags == ["Management"]
        env.security[0]["BEARER"] == ["ADMIN"]
    }
}
