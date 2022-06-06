package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiOperationTagsSpec extends AbstractOpenApiTypeElementSpec {


    void "test build OpenAPI operation with @Tags"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Controller("/path")
class OpenApiController {

    @Get(uri = "/200")
    @Tags({
            @Tag(
                    name = "Tag 0",
                    description = "desc 0",
                    externalDocs = @ExternalDocumentation(
                            description = "docs desc0",
                            url = "http://externaldoc.com",
                            extensions = {
                                    @Extension(
                                            name = "extdocs.custom1",
                                            properties = {
                                                    @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                                    @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                            }
                                    ),
                                    @Extension(
                                            name = "extdocs.custom2",
                                            properties = {
                                                    @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                                    @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                            }
                                    )
                            }
                    ),
                    extensions = {
                            @Extension(
                                    name = "tag.custom1",
                                    properties = {
                                            @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                            @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                    }
                            ),
                            @Extension(
                                    name = "tag.custom2",
                                    properties = {
                                            @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                            @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                    }
                            )
                    }
            ),
            @Tag(name = "Tag 1", description = "desc 1", externalDocs = @ExternalDocumentation(description = "docs desc")),
            @Tag(name = "Tag 2", description = "desc 2", externalDocs = @ExternalDocumentation(description = "docs desc 2")),
            @Tag(name = "Tag 3")
    })
    public String processSync() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths.get("/path/200").get

        then:
        operation
        operation.tags
        operation.tags.size() == 4
        operation.tags.get(0) == "Tag 0"
        operation.tags.get(1) == "Tag 1"
        operation.tags.get(2) == "Tag 2"
        operation.tags.get(3) == "Tag 3"

        openAPI.tags
        openAPI.tags.size() == 3
        openAPI.tags.get(0).name == "Tag 0"
        openAPI.tags.get(0).description == 'desc 0'
        openAPI.tags.get(0).externalDocs
        openAPI.tags.get(0).externalDocs.description == "docs desc0"
        openAPI.tags.get(0).externalDocs.url == "http://externaldoc.com"
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom1'.'prop11' == 'prop11Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom1'.'prop12' == 'prop12Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom2'.'prop21' == 'prop21Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom2'.'prop22' == 'prop22Val'
        openAPI.tags.get(0).extensions.'x-tag.custom1'.'prop11' == 'prop11Val'
        openAPI.tags.get(0).extensions.'x-tag.custom1'.'prop12' == 'prop12Val'
        openAPI.tags.get(0).extensions.'x-tag.custom2'.'prop21' == 'prop21Val'
        openAPI.tags.get(0).extensions.'x-tag.custom2'.'prop22' == 'prop22Val'

        openAPI.tags.get(1).name == "Tag 1"
        openAPI.tags.get(1).description == 'desc 1'
        openAPI.tags.get(1).externalDocs
        openAPI.tags.get(1).externalDocs.description == "docs desc"

        openAPI.tags.get(2).name == "Tag 2"
        openAPI.tags.get(2).description == 'desc 2'
        openAPI.tags.get(2).externalDocs
        openAPI.tags.get(2).externalDocs.description == "docs desc 2"
    }

    void "test build OpenAPI operation with @Tag repeated"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Controller("/path")
class OpenApiController {

    @Get(uri = "/200")
    @Tag(
            name = "Tag 0",
            description = "desc 0",
            externalDocs = @ExternalDocumentation(
                    description = "docs desc0",
                    url = "http://externaldoc.com",
                    extensions = {
                            @Extension(
                                    name = "extdocs.custom1",
                                    properties = {
                                            @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                            @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                    }
                            ),
                            @Extension(
                                    name = "extdocs.custom2",
                                    properties = {
                                            @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                            @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                    }
                            )
                    }
            ),
            extensions = {
                    @Extension(
                            name = "tag.custom1",
                            properties = {
                                    @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                    @ExtensionProperty(name = "prop12", value = "prop12Val"),
                            }
                    ),
                    @Extension(
                            name = "tag.custom2",
                            properties = {
                                    @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                    @ExtensionProperty(name = "prop22", value = "prop22Val"),
                            }
                    )
            }
    )
    @Tag(name = "Tag 1", description = "desc 1", externalDocs = @ExternalDocumentation(description = "docs desc"))
    @Tag(name = "Tag 2", description = "desc 2", externalDocs = @ExternalDocumentation(description = "docs desc 2"))
    @Tag(name = "Tag 3")
    public String processSync() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths.get("/path/200").get

        then:
        operation
        operation.tags
        operation.tags.size() == 4
        operation.tags.get(0) == "Tag 0"
        operation.tags.get(1) == "Tag 1"
        operation.tags.get(2) == "Tag 2"
        operation.tags.get(3) == "Tag 3"

        openAPI.tags
        openAPI.tags.size() == 3
        openAPI.tags.get(0).name == "Tag 0"
        openAPI.tags.get(0).description == 'desc 0'
        openAPI.tags.get(0).externalDocs
        openAPI.tags.get(0).externalDocs.description == "docs desc0"
        openAPI.tags.get(0).externalDocs.url == "http://externaldoc.com"
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom1'.'prop11' == 'prop11Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom1'.'prop12' == 'prop12Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom2'.'prop21' == 'prop21Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom2'.'prop22' == 'prop22Val'
        openAPI.tags.get(0).extensions.'x-tag.custom1'.'prop11' == 'prop11Val'
        openAPI.tags.get(0).extensions.'x-tag.custom1'.'prop12' == 'prop12Val'
        openAPI.tags.get(0).extensions.'x-tag.custom2'.'prop21' == 'prop21Val'
        openAPI.tags.get(0).extensions.'x-tag.custom2'.'prop22' == 'prop22Val'

        openAPI.tags.get(1).name == "Tag 1"
        openAPI.tags.get(1).description == 'desc 1'
        openAPI.tags.get(1).externalDocs
        openAPI.tags.get(1).externalDocs.description == "docs desc"

        openAPI.tags.get(2).name == "Tag 2"
        openAPI.tags.get(2).description == 'desc 2'
        openAPI.tags.get(2).externalDocs
        openAPI.tags.get(2).externalDocs.description == "docs desc 2"
    }

    void "test build OpenAPI operation with class tags"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Tags({
        @Tag(
                name = "Tag 01",
                description = "desc 0",
                externalDocs = @ExternalDocumentation(
                        description = "docs desc0",
                        url = "http://externaldoc.com",
                        extensions = {
                                @Extension(
                                        name = "extdocs.custom1",
                                        properties = {
                                                @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                                @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                        }
                                ),
                                @Extension(
                                        name = "extdocs.custom2",
                                        properties = {
                                                @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                                @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                        }
                                )
                        }
                ),
                extensions = {
                        @Extension(
                                name = "tag.custom1",
                                properties = {
                                        @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                        @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                }
                        ),
                        @Extension(
                                name = "tag.custom2",
                                properties = {
                                        @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                        @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                }
                        )
                }
        ),
        @Tag(name = "Tag 11", description = "desc 1", externalDocs = @ExternalDocumentation(description = "docs desc")),
        @Tag(name = "Tag 21", description = "desc 2", externalDocs = @ExternalDocumentation(description = "docs desc 2")),
        @Tag(name = "Tag 31")
})
@Controller("/path")
class OpenApiController {

    @Get(uri = "/200")
    @Tags({
            @Tag(
                    name = "Tag 0",
                    description = "desc 0",
                    externalDocs = @ExternalDocumentation(
                            description = "docs desc0",
                            url = "http://externaldoc.com",
                            extensions = {
                                    @Extension(
                                            name = "extdocs.custom1",
                                            properties = {
                                                    @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                                    @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                            }
                                    ),
                                    @Extension(
                                            name = "extdocs.custom2",
                                            properties = {
                                                    @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                                    @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                            }
                                    )
                            }
                    ),
                    extensions = {
                            @Extension(
                                    name = "tag.custom1",
                                    properties = {
                                            @ExtensionProperty(name = "prop11", value = "prop11Val"),
                                            @ExtensionProperty(name = "prop12", value = "prop12Val"),
                                    }
                            ),
                            @Extension(
                                    name = "tag.custom2",
                                    properties = {
                                            @ExtensionProperty(name = "prop21", value = "prop21Val"),
                                            @ExtensionProperty(name = "prop22", value = "prop22Val"),
                                    }
                            )
                    }
            ),
            @Tag(name = "Tag 1", description = "desc 1", externalDocs = @ExternalDocumentation(description = "docs desc")),
            @Tag(name = "Tag 2", description = "desc 2", externalDocs = @ExternalDocumentation(description = "docs desc 2")),
            @Tag(name = "Tag 3")
    })
    public String processSync() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths.get("/path/200").get

        then:
        operation
        operation.tags
        operation.tags.size() == 8
        operation.tags.get(0) == "Tag 0"
        operation.tags.get(1) == "Tag 1"
        operation.tags.get(2) == "Tag 2"
        operation.tags.get(3) == "Tag 3"
        operation.tags.get(4) == "Tag 01"
        operation.tags.get(5) == "Tag 11"
        operation.tags.get(6) == "Tag 21"
        operation.tags.get(7) == "Tag 31"

        openAPI.tags
        openAPI.tags.size() == 6
        openAPI.tags.get(0).name == "Tag 0"
        openAPI.tags.get(0).description == 'desc 0'
        openAPI.tags.get(0).externalDocs
        openAPI.tags.get(0).externalDocs.description == "docs desc0"
        openAPI.tags.get(0).externalDocs.url == "http://externaldoc.com"
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom1'.'prop11' == 'prop11Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom1'.'prop12' == 'prop12Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom2'.'prop21' == 'prop21Val'
        openAPI.tags.get(0).externalDocs.extensions.'x-extdocs.custom2'.'prop22' == 'prop22Val'
        openAPI.tags.get(0).extensions.'x-tag.custom1'.'prop11' == 'prop11Val'
        openAPI.tags.get(0).extensions.'x-tag.custom1'.'prop12' == 'prop12Val'
        openAPI.tags.get(0).extensions.'x-tag.custom2'.'prop21' == 'prop21Val'
        openAPI.tags.get(0).extensions.'x-tag.custom2'.'prop22' == 'prop22Val'

        openAPI.tags.get(1).name == "Tag 1"
        openAPI.tags.get(1).description == 'desc 1'
        openAPI.tags.get(1).externalDocs
        openAPI.tags.get(1).externalDocs.description == "docs desc"

        openAPI.tags.get(2).name == "Tag 2"
        openAPI.tags.get(2).description == 'desc 2'
        openAPI.tags.get(2).externalDocs
        openAPI.tags.get(2).externalDocs.description == "docs desc 2"

        openAPI.tags.get(3).name == "Tag 01"
        openAPI.tags.get(3).description == 'desc 0'
        openAPI.tags.get(3).externalDocs
        openAPI.tags.get(3).externalDocs.description == "docs desc0"
        openAPI.tags.get(3).externalDocs.url == "http://externaldoc.com"
        openAPI.tags.get(3).externalDocs.extensions.'x-extdocs.custom1'.'prop11' == 'prop11Val'
        openAPI.tags.get(3).externalDocs.extensions.'x-extdocs.custom1'.'prop12' == 'prop12Val'
        openAPI.tags.get(3).externalDocs.extensions.'x-extdocs.custom2'.'prop21' == 'prop21Val'
        openAPI.tags.get(3).externalDocs.extensions.'x-extdocs.custom2'.'prop22' == 'prop22Val'
        openAPI.tags.get(3).extensions.'x-tag.custom1'.'prop11' == 'prop11Val'
        openAPI.tags.get(3).extensions.'x-tag.custom1'.'prop12' == 'prop12Val'
        openAPI.tags.get(3).extensions.'x-tag.custom2'.'prop21' == 'prop21Val'
        openAPI.tags.get(3).extensions.'x-tag.custom2'.'prop22' == 'prop22Val'

        openAPI.tags.get(4).name == "Tag 11"
        openAPI.tags.get(4).description == 'desc 1'
        openAPI.tags.get(4).externalDocs
        openAPI.tags.get(4).externalDocs.description == "docs desc"

        openAPI.tags.get(5).name == "Tag 21"
        openAPI.tags.get(5).description == 'desc 2'
        openAPI.tags.get(5).externalDocs
        openAPI.tags.get(5).externalDocs.description == "docs desc 2"
    }

    void "test build OpenAPI operation with simple tags"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Controller("/path")
class OpenApiController {

    @Get(uri = "/200")
    @Tags({
            @Tag(name = "Tag 0"),
            @Tag(name = "Complex tag", description = "this is description"),
            @Tag(name = "Tag 2"),
            @Tag(name = "Tag 3")
    })
    public String processSync() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        AbstractOpenApiVisitor.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = AbstractOpenApiVisitor.testReference
        Operation operation = openAPI.paths.get("/path/200").get

        then:
        operation
        operation.tags
        operation.tags.size() == 4
        operation.tags.get(0) == "Tag 0"
        operation.tags.get(1) == "Complex tag"
        operation.tags.get(2) == "Tag 2"
        operation.tags.get(3) == "Tag 3"

        openAPI.tags
        openAPI.tags.size() == 1
        openAPI.tags.get(0).name == "Complex tag"
        openAPI.tags.get(0).description == "this is description"
    }

}
