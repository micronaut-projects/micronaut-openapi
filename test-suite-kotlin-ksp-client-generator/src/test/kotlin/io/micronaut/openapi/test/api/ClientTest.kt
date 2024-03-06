package io.micronaut.openapi.test.api

import io.micronaut.openapi.test.util.TestUtils.assertFileContains
import org.junit.jupiter.api.Test

class ClientTest {

    private val outputPath: String = "build/generated/openapi"

    @Test
    fun testClientId() {
        assertFileContains("$outputPath/src/main/kotlin/io/micronaut/openapi/test/api/PetApi.kt",
                "@Client(id = \"myClient\", path = \"\\\${myClient.base-path}\")")
    }
}
