package io.micronaut.openapi.spring

// tag::imports[]
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// end::imports[]
// tag::clazz[]
@Configuration
open class WebConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/swagger/views/swagger-ui/")
        registry.addResourceHandler("/swagger/**")
            .addResourceLocations("classpath:/META-INF/swagger/")
    }
}
//end::clazz[]
