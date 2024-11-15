package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

class OpenApiOperationTagsSpec extends AbstractOpenApiTypeElementSpec {

    void "test build OpenAPI operation with @Tags"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

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
                            url = "https://externaldoc.com",
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
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
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
        openAPI.tags.get(0).externalDocs.url == "https://externaldoc.com"
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
                    url = "https://externaldoc.com",
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
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
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
        openAPI.tags.get(0).externalDocs.url == "https://externaldoc.com"
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
                        url = "https://externaldoc.com",
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
                            url = "https://externaldoc.com",
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
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path/200").get

        then:
        operation
        operation.tags
        operation.tags.size() == 8
        operation.tags.contains("Tag 0")
        operation.tags.contains("Tag 1")
        operation.tags.contains("Tag 2")
        operation.tags.contains("Tag 3")
        operation.tags.contains("Tag 01")
        operation.tags.contains("Tag 11")
        operation.tags.contains("Tag 21")
        operation.tags.contains("Tag 31")

        openAPI.tags
        openAPI.tags.size() == 6
        openAPI.tags.get(0).name == "Tag 0"
        openAPI.tags.get(0).description == 'desc 0'
        openAPI.tags.get(0).externalDocs
        openAPI.tags.get(0).externalDocs.description == "docs desc0"
        openAPI.tags.get(0).externalDocs.url == "https://externaldoc.com"
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
        openAPI.tags.get(3).externalDocs.url == "https://externaldoc.com"
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

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
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
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path/200").get

        then:
        operation
        operation.tags
        operation.tags.size() == 4
        operation.tags.contains("Tag 0")
        operation.tags.contains("Complex tag")
        operation.tags.contains("Tag 2")
        operation.tags.contains("Tag 3")

        openAPI.tags
        openAPI.tags.size() == 1
        openAPI.tags.get(0).name == "Complex tag"
        openAPI.tags.get(0).description == "this is description"
    }

    void "test check tags sorting"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Tag(name = "C Tag")
@Controller("/path")
class OpenApiController {

    @Get(uri = "/200")
    @Tag(name = "Z Tag")
    @Tags({
            @Tag(name = "S Tag"),
            @Tag(name = "A Tag"),
            @Tag(name = "B Tag", description = "this is description")
    })
    public String processSync() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path/200").get

        then:
        operation
        operation.tags
        operation.tags.size() == 5
        operation.tags.get(0) == "A Tag"
        operation.tags.get(1) == "B Tag"
        operation.tags.get(2) == "C Tag"
        operation.tags.get(3) == "S Tag"
        operation.tags.get(4) == "Z Tag"

        openAPI.tags
        openAPI.tags.size() == 1
        openAPI.tags.get(0).name == "B Tag"
        openAPI.tags.get(0).description == "this is description"
    }

    void "test @Tag annotations"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Interface class Tag")
interface OpenApiInterface {

    @Tag(name = "Interface Method tag")
    String processSync();
}

@Tag(name = "Super Class Tag")
class OpenApiSuperController {

    @Tag(name = "Super Method tag")
    @Get(uri = "/200")
    public String processSync() {
        return null;
    }
}

@Tag(name = "Class Tag")
@Controller("/path")
class OpenApiController extends OpenApiSuperController implements OpenApiInterface {

    @Tag(name = "Method tag")
    @Get(uri = "/200")
    @Override
    public String processSync() {
        return super.processSync();
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then: "the state is correct"
        Utils.testReference != null

        when: "The OpenAPI is retrieved"
        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths.get("/path/200").get

        then:
        operation
        operation.tags.size() == 6
        operation.tags.containsAll(List.of(
                "Class Tag",
                "Method tag",
                "Super Class Tag",
                "Super Method tag",
                "Interface Method tag",
                "Interface class Tag"
        ))
    }
}
