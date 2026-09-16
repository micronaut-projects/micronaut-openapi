package io.micronaut.configuration.openapi.docs.extraschema.exclude

import io.micronaut.openapi.annotation.OpenAPIExtraSchema

@OpenAPIExtraSchema
class ExcludedByPackage {

    var field1: String? = null
}
