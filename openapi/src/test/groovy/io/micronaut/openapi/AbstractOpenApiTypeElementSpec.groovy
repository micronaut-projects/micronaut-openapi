package io.micronaut.openapi

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.openapi.visitor.OpenApiApplicationVisitor
import io.micronaut.openapi.visitor.Utils

abstract class AbstractOpenApiTypeElementSpec extends AbstractTypeElementSpec {

    def setup() {
        Utils.setAllKnownVersions(null)
        Utils.setAllKnownGroups(null)
        Utils.setEndpointInfos(null)
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_ENABLED)
        System.setProperty(Utils.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        Utils.setAllKnownVersions(null)
        Utils.setAllKnownGroups(null)
        Utils.setEndpointInfos(null)
        System.clearProperty(Utils.ATTR_TEST_MODE)
    }

}
