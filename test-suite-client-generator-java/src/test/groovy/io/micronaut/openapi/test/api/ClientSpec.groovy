package io.micronaut.openapi.test.api

import spock.lang.Specification

import static io.micronaut.openapi.test.util.TestUtils.assertFileContains

class ClientSpec extends Specification {

    String outputPath = "build/generated/openapi"

    void "test client id"() {
        expect:
        assertFileContains(outputPath + "/src/main/java/io/micronaut/openapi/test/api/PetApi.java",
                '@Client(id = "myClient", path = "${myClient.base-path}")');
    }
}
