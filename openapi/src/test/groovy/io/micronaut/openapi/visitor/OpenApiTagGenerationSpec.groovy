package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import spock.util.environment.RestoreSystemProperties

import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_BY_PACKAGE_ENABLED
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_DESCRIPTION_MAX_LENGTH
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_NAMING_STRATEGY
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_POSTFIXES
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_PREFIXES

class OpenApiTagGenerationSpec extends AbstractOpenApiTypeElementSpec {

    @RestoreSystemProperties
    void "test generation tags"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED, "true")
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User operations
 */
@Controller
class UserOperationsController {

    @Post("/user")
    void create() {
    }

    @Get("/user")
    void get() {
    }
}

/**
 * Company operations
 */
@Tag(name = "custom")
@Controller
class CompanyOperationsController {

    @Post("/company")
    void create() {
    }

    @Get("/company")
    void get() {
    }
}

@OpenAPIDefinition(
    tags = {
        @Tag(name = "custom", description = "This is custom tag description")
    }
)
class Application {}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def openApi = Utils.testReference

        then:
        openApi.tags.size() == 2
        openApi.tags[0].name == "custom"
        openApi.tags[0].description == "This is custom tag description"
        openApi.tags[1].name == "UserOperations"
        openApi.tags[1].description == "User operations"

        openApi.paths.'/user'.get.tags
        openApi.paths.'/user'.get.tags.size() == 1
        openApi.paths.'/user'.get.tags[0] == "UserOperations"
        openApi.paths.'/user'.post.tags
        openApi.paths.'/user'.post.tags.size() == 1
        openApi.paths.'/user'.post.tags[0] == "UserOperations"

        openApi.paths.'/company'.get.tags
        openApi.paths.'/company'.get.tags.size() == 1
        openApi.paths.'/company'.get.tags[0] == "custom"
        openApi.paths.'/company'.post.tags
        openApi.paths.'/company'.post.tags.size() == 1
        openApi.paths.'/company'.post.tags[0] == "custom"
    }

    @RestoreSystemProperties
    void "test generation tags with custom naming strategy"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED, "true")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_NAMING_STRATEGY, "KEBAB_CASE")
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User operations
 */
@Controller
class UserOperationsController {

    @Post("/user")
    void create() {
    }

    @Get("/user")
    void get() {
    }
}

/**
 * Company operations
 */
@Tag(name = "custom")
@Controller
class CompanyOperationsController {

    @Post("/company")
    void create() {
    }

    @Get("/company")
    void get() {
    }
}

@OpenAPIDefinition(
    tags = {
        @Tag(name = "custom", description = "This is custom tag description")
    }
)
class Application {}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def openApi = Utils.testReference

        then:
        openApi.tags.size() == 2
        openApi.tags[0].name == "custom"
        openApi.tags[0].description == "This is custom tag description"
        openApi.tags[1].name == "user-operations"
        openApi.tags[1].description == "User operations"

        openApi.paths.'/user'.get.tags
        openApi.paths.'/user'.get.tags.size() == 1
        openApi.paths.'/user'.get.tags[0] == "user-operations"
        openApi.paths.'/user'.post.tags
        openApi.paths.'/user'.post.tags.size() == 1
        openApi.paths.'/user'.post.tags[0] == "user-operations"

        openApi.paths.'/company'.get.tags
        openApi.paths.'/company'.get.tags.size() == 1
        openApi.paths.'/company'.get.tags[0] == "custom"
        openApi.paths.'/company'.post.tags
        openApi.paths.'/company'.post.tags.size() == 1
        openApi.paths.'/company'.post.tags[0] == "custom"
    }

    @RestoreSystemProperties
    void "test generation tags without removing postfixes"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED, "true")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_NAMING_STRATEGY, "KEBAB_CASE")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_POSTFIXES, "")
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User operations
 */
@Controller
class UserOperationsController {

    @Post("/user")
    void create() {
    }

    @Get("/user")
    void get() {
    }
}

/**
 * Company operations
 */
@Tag(name = "custom")
@Controller
class CompanyOperationsController {

    @Post("/company")
    void create() {
    }

    @Get("/company")
    void get() {
    }
}

@OpenAPIDefinition(
    tags = {
        @Tag(name = "custom", description = "This is custom tag description")
    }
)
class Application {}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def openApi = Utils.testReference

        then:
        openApi.tags.size() == 2
        openApi.tags[0].name == "custom"
        openApi.tags[0].description == "This is custom tag description"
        openApi.tags[1].name == "user-operations-controller"
        openApi.tags[1].description == "User operations"

        openApi.paths.'/user'.get.tags
        openApi.paths.'/user'.get.tags.size() == 1
        openApi.paths.'/user'.get.tags[0] == "user-operations-controller"
        openApi.paths.'/user'.post.tags
        openApi.paths.'/user'.post.tags.size() == 1
        openApi.paths.'/user'.post.tags[0] == "user-operations-controller"

        openApi.paths.'/company'.get.tags
        openApi.paths.'/company'.get.tags.size() == 1
        openApi.paths.'/company'.get.tags[0] == "custom"
        openApi.paths.'/company'.post.tags
        openApi.paths.'/company'.post.tags.size() == 1
        openApi.paths.'/company'.post.tags[0] == "custom"
    }

    @RestoreSystemProperties
    void "test generation tags removing prefixes"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED, "true")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_NAMING_STRATEGY, "KEBAB_CASE")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_PREFIXES, "user")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_POSTFIXES, "")
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User operations
 */
@Controller
class UserOperationsController {

    @Post("/user")
    void create() {
    }

    @Get("/user")
    void get() {
    }
}

/**
 * Company operations
 */
@Tag(name = "custom")
@Controller
class CompanyOperationsController {

    @Post("/company")
    void create() {
    }

    @Get("/company")
    void get() {
    }
}

@OpenAPIDefinition(
    tags = {
        @Tag(name = "custom", description = "This is custom tag description")
    }
)
class Application {}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def openApi = Utils.testReference

        then:
        openApi.tags.size() == 2
        openApi.tags[0].name == "custom"
        openApi.tags[0].description == "This is custom tag description"
        openApi.tags[1].name == "operations-controller"
        openApi.tags[1].description == "User operations"

        openApi.paths.'/user'.get.tags
        openApi.paths.'/user'.get.tags.size() == 1
        openApi.paths.'/user'.get.tags[0] == "operations-controller"
        openApi.paths.'/user'.post.tags
        openApi.paths.'/user'.post.tags.size() == 1
        openApi.paths.'/user'.post.tags[0] == "operations-controller"

        openApi.paths.'/company'.get.tags
        openApi.paths.'/company'.get.tags.size() == 1
        openApi.paths.'/company'.get.tags[0] == "custom"
        openApi.paths.'/company'.post.tags
        openApi.paths.'/company'.post.tags.size() == 1
        openApi.paths.'/company'.post.tags[0] == "custom"
    }

    @RestoreSystemProperties
    void "test generation tags, when controller or operations with @Tag annotation"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED, "true")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_NAMING_STRATEGY, "KEBAB_CASE")
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User operations
 */
@Controller
class UserOperationsController {

    @Tag(name = "user-custom")
    @Post("/user")
    void create() {
    }

    @Get("/user")
    void get() {
    }
}

/**
 * Company operations
 */
@Tag(name = "custom")
@Controller
class CompanyOperationsController {

    @Post("/company")
    void create() {
    }

    @Tag(name = "custom-company")
    @Get("/company")
    void get() {
    }
}

@OpenAPIDefinition(
    tags = {
        @Tag(name = "custom", description = "This is custom tag description")
    }
)
class Application {}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def openApi = Utils.testReference

        then:
        openApi.tags.size() == 2
        openApi.tags[0].name == "custom"
        openApi.tags[0].description == "This is custom tag description"
        openApi.tags[1].name == "user-operations"
        openApi.tags[1].description == "User operations"

        openApi.paths.'/user'.get.tags
        openApi.paths.'/user'.get.tags.size() == 1
        openApi.paths.'/user'.get.tags[0] == "user-operations"
        openApi.paths.'/user'.post.tags
        openApi.paths.'/user'.post.tags.size() == 2
        openApi.paths.'/user'.post.tags[0] == "user-custom"
        openApi.paths.'/user'.post.tags[1] == "user-operations"

        openApi.paths.'/company'.get.tags
        openApi.paths.'/company'.get.tags.size() == 2
        openApi.paths.'/company'.get.tags[0] == "custom"
        openApi.paths.'/company'.get.tags[1] == "custom-company"
        openApi.paths.'/company'.post.tags
        openApi.paths.'/company'.post.tags.size() == 1
        openApi.paths.'/company'.post.tags[0] == "custom"
    }

    @RestoreSystemProperties
    void "test generation tags with description max length"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED, "true")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_DESCRIPTION_MAX_LENGTH, "50")
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User operations or user operations or user operations or user operations or user operations or user operations or user operations or user operations
 */
@Controller
class UserOperationsController {

    @Post("/user")
    void create() {
    }

    @Get("/user")
    void get() {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def openApi = Utils.testReference

        then:
        openApi.tags.size() == 1
        openApi.tags[0].name == "UserOperations"
        openApi.tags[0].description.size() == 50
        openApi.tags[0].description == StringUtil.left("User operations or user operations or user operations or user operations or user operations or user operations or user operations or user operations", 50)
    }

    @RestoreSystemProperties
    void "test generation tags by controller disabled"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED, "false")
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User operations
 */
@Controller
class UserOperationsController {

    @Post("/user")
    void create() {
    }

    @Get("/user")
    void get() {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def openApi = Utils.testReference

        then:
        !openApi.tags
    }

    @RestoreSystemProperties
    void "test generation tags by package enabled"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_PACKAGE_ENABLED, "true")
        buildBeanDefinition('io.micronaut.openapi.tags.users.operations.MyBean', '''
package io.micronaut.openapi.tags.users.operations;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User operations
 */
@Controller
class UserOperationsController {

    @Post("/user")
    void create() {
    }

    @Get("/user")
    void get() {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def openApi = Utils.testReference

        then:
        openApi.paths.'/user'.get.tags
        openApi.paths.'/user'.get.tags.size() == 1
        openApi.paths.'/user'.get.tags[0] == "io.micronaut.openapi.tags.users.operations"
        openApi.paths.'/user'.post.tags
        openApi.paths.'/user'.post.tags.size() == 1
        openApi.paths.'/user'.post.tags[0] == "io.micronaut.openapi.tags.users.operations"
    }

    @RestoreSystemProperties
    void "test generation tags by package enabled with modifications"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_PACKAGE_ENABLED, "true")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_PREFIXES, "io.micronaut.openapi.tags.")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_NAMING_STRATEGY, "KEBAB_CASE")
        buildBeanDefinition('io.micronaut.openapi.tags.user.operations.MyBean', '''
package io.micronaut.openapi.tags.user.operations;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User operations
 */
@Controller
class UserOperationsController {

    @Post("/user")
    void create() {
    }

    @Get("/user")
    void get() {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def openApi = Utils.testReference

        then:
        openApi.paths.'/user'.get.tags
        openApi.paths.'/user'.get.tags.size() == 1
        openApi.paths.'/user'.get.tags[0] == "user-operations"
        openApi.paths.'/user'.post.tags
        openApi.paths.'/user'.post.tags.size() == 1
        openApi.paths.'/user'.post.tags[0] == "user-operations"
    }

    @RestoreSystemProperties
    void "test generation tags by package and by class name together"() {
        given:
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_CLASS_ENABLED, "true")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_BY_PACKAGE_ENABLED, "true")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_REMOVE_PREFIXES, "io.micronaut.openapi.tags.")
        System.setProperty(MICRONAUT_OPENAPI_TAG_GENERATION_NAMING_STRATEGY, "KEBAB_CASE")
        buildBeanDefinition('io.micronaut.openapi.tags.user.operations.MyBean', '''
package io.micronaut.openapi.tags.user.operations;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User operations
 */
@Controller
class UserOperationsController {

    @Post("/user")
    void create() {
    }

    @Get("/user")
    void get() {
    }
}

/**
 * Second user operations
 */
@Controller
class SecondUserOperationsController {

    @Post("/user2")
    void create() {
    }

    @Get("/user2")
    void get() {
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        when:
        def openApi = Utils.testReference

        then:
        openApi.tags.size() == 2
        openApi.tags[0].name == "user-operations"
        openApi.tags[0].description == "User operations"
        openApi.tags[1].name == "second-user-operations"
        openApi.tags[1].description == "Second user operations"

        openApi.paths.'/user'.get.tags
        openApi.paths.'/user'.get.tags.size() == 1
        openApi.paths.'/user'.get.tags[0] == "user-operations"
        openApi.paths.'/user'.post.tags
        openApi.paths.'/user'.post.tags.size() == 1
        openApi.paths.'/user'.post.tags[0] == "user-operations"

        openApi.paths.'/user2'.get.tags
        openApi.paths.'/user2'.get.tags.size() == 2
        openApi.paths.'/user2'.get.tags[0] == "second-user-operations"
        openApi.paths.'/user2'.get.tags[1] == "user-operations"
        openApi.paths.'/user2'.post.tags
        openApi.paths.'/user2'.post.tags.size() == 2
        openApi.paths.'/user2'.post.tags[0] == "second-user-operations"
        openApi.paths.'/user2'.post.tags[1] == "user-operations"
    }
}
