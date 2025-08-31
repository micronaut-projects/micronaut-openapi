package io.micronaut.openapi.spring

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
@OpenAPIDefinition(info = Info(version = "\${app.version}"))
open class Application

fun main(args: Array<String>) {
    val application = SpringApplication(Application::class.java)
    application.setBannerMode(Banner.Mode.OFF)
    application.run(*args)
}
