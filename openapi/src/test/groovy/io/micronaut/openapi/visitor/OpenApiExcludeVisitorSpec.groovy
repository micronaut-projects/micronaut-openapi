package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import spock.util.environment.RestoreSystemProperties

class OpenApiExcludeVisitorSpec extends AbstractOpenApiTypeElementSpec {

    void "test OpenApiExclude"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.api.IncludedApi;
import io.micronaut.api.PublicApi;
import io.micronaut.api.internal.InternalApi;
import io.micronaut.openapi.annotation.OpenAPIExclude;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import jakarta.inject.Singleton;

@OpenAPIInclude(
    packages = {
        "io.micronaut.api.excluded",
        "io.micronaut.api.excluded.subpackage",
    },
    classes = {
        InternalApi.class,
        PublicApi.class,
        IncludedApi.class,
    }
)
@OpenAPIExclude(
    packages = {
        "io.micronaut.api.excluded",
        "io.micronaut.api.excluded.subpackage",
    },
    classes = {
        InternalApi.class,
        PublicApi.class,
    }
)
class Application {

}

@Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openApi = Utils.testReference

        then:
        openApi.info != null
        openApi.paths.size() == 1
        // only IncludedApi controller
        openApi.paths."/public/get-info"
        openApi.paths."/public/get-info".get
        openApi.paths."/public/get-info".get.operationId == "getInfo"
    }

    @RestoreSystemProperties
    void "test OpenApiExclude by properties"() {

        setup:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_EXCLUDE_CLASSES, "io.micronaut.api.internal.InternalApi,io.micronaut.api.PublicApi")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_EXCLUDE_PACKAGES, "io.micronaut.api.excluded,io.micronaut.api.excluded.subpackage")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.api.IncludedApi;
import io.micronaut.api.PublicApi;
import io.micronaut.api.internal.InternalApi;
import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.inject.Singleton;

@OpenAPIInclude(
    packages = {
        "io.micronaut.api.excluded",
        "io.micronaut.api.excluded.subpackage",
    },
    classes = {
        InternalApi.class,
        PublicApi.class,
        IncludedApi.class,
    }
)
@OpenAPIDefinition(
    info = @Info(
        title = "The openAPI",
        version = "1.0.0"
    )
)
class Application {

}

@Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openApi = Utils.testReference

        then:
        openApi.info != null
        openApi.paths.size() == 1
        // only IncludedApi controller
        openApi.paths."/public/get-info"
        openApi.paths."/public/get-info".get
        openApi.paths."/public/get-info".get.operationId == "getInfo"
    }
}
