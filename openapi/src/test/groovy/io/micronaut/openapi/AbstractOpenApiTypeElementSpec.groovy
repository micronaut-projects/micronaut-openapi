package io.micronaut.openapi

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.openapi.visitor.AbstractOpenApiVisitor

abstract class AbstractOpenApiTypeElementSpec extends AbstractTypeElementSpec {

    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    def cleanup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "")
    }

}
