package io.micronaut.configuration.openapi.docs

import spock.lang.Specification

class OpenApiExtraSchemaSpec extends Specification {

    void "extra schemas are added"() {
        when:
        def schemas = OpenApiSpec.load().components.schemas

        then:
        schemas.containsKey("UnusedSchema")
        schemas.containsKey("UnusedModel1")
        // the Groovy AST transformation cannot enumerate the classes of a source package, so `packages = [...]` only applies to compiled classes
        !schemas.containsKey("ExtraModel")
        !schemas.containsKey("ExcludedModel")
        !schemas.containsKey("ExcludedByPackage")
    }
}
