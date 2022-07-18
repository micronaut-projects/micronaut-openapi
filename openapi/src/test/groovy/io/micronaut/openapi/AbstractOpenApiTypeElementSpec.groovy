package io.micronaut.openapi

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.openapi.visitor.Utils

abstract class AbstractOpenApiTypeElementSpec extends AbstractTypeElementSpec {

    def setup() {
        System.setProperty(Utils.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        System.setProperty(Utils.ATTR_TEST_MODE, "")
    }

}
