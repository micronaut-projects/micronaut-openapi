package io.micronaut.configuration.openapi.docs.extraschema

import io.micronaut.openapi.annotation.OpenAPIExtraSchema

@OpenAPIExtraSchema
class ExcludedModel {

    var field1: String? = null
}
