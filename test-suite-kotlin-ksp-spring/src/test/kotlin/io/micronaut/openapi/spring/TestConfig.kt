package io.micronaut.openapi.spring

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
open class TestConfig {

    @Bean
    open fun restClient(@Value("\${server.port:8080}") port: Int): RestClient {
        return RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()
    }

    companion object {
        const val APP_NAME: String = "test-suite-kotlin-ksp-spring"
        const val APP_VERSION: String = "myVersion"
    }
}
