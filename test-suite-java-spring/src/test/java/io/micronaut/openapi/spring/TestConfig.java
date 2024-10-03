package io.micronaut.openapi.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TestConfig {

    @Bean
    RestClient restClient(@Value("${server.port:8080}") int port) {
        return RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .build();
    }
}
