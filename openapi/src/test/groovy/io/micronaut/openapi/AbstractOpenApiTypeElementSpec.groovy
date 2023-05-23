package io.micronaut.openapi

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.openapi.visitor.OpenApiApplicationVisitor
import io.micronaut.openapi.visitor.Utils

abstract class AbstractOpenApiTypeElementSpec extends AbstractTypeElementSpec {

    def setup() {
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ENABLED)
        System.setProperty(Utils.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        System.clearProperty(Utils.ATTR_TEST_MODE)
    }

}
