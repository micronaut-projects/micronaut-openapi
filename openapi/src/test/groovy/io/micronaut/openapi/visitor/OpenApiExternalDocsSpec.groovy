package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema

class OpenApiExternalDocsSpec extends AbstractOpenApiTypeElementSpec {

    void "test OpenAPI ExternalDocs on operation level"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;

@Controller("/path")
class MyController {

    @ExternalDocumentation(
            description = "operation external docs",
            url = "http://externalUrl",
            extensions = {
                    @Extension(
                            name = "myExt1",
                            properties = {
                                    @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                    @ExtensionProperty(name = "prop2", value = "prop2Val"),
                            }
                    ),
                    @Extension(
                            name = "myExt2",
                            properties = {
                                    @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                    @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                            }
                    ),
            }
    )
    @Post
    public void processSync(@Body MyDto myDto) {
    }

}

class MyDto {

    private Parameters parameters;

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

}

class Parameters {

    private Integer stampWidth;
    private Integer stampHeight;
    private int pageNumber;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }

    public Integer getStampHeight() {
        return stampHeight;
    }

    public void setStampHeight(Integer stampHeight) {
        this.stampHeight = stampHeight;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation = openAPI.paths?.get("/path")?.post

        expect:
        operation
        operation.externalDocs
        operation.externalDocs.description == 'operation external docs'
        operation.externalDocs.url == 'http://externalUrl'
        operation.externalDocs.extensions.'x-myExt1'.prop1 == "prop1Val"
        operation.externalDocs.extensions.'x-myExt1'.prop2 == "prop2Val"
        operation.externalDocs.extensions.'x-myExt2'.prop1 == "prop1Val1"
        operation.externalDocs.extensions.'x-myExt2'.prop2 == "prop2Val2"
    }

    void "test OpenAPI ExternalDocs on controller level"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;

@ExternalDocumentation(
        description = "operation external docs",
        url = "http://externalUrl",
        extensions = {
                @Extension(
                        name = "myExt1",
                        properties = {
                                @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                @ExtensionProperty(name = "prop2", value = "prop2Val"),
                        }
                ),
                @Extension(
                        name = "myExt2",
                        properties = {
                                @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                        }
                ),
        }
)
@Controller
class MyController {

    @Post("path1")
    public void processSync(@Body MyDto myDto) {
    }

    @Post("path2")
    public void processSync2(@Body MyDto myDto) {
    }
}

class MyDto {

    private Parameters parameters;

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

}

class Parameters {

    private Integer stampWidth;
    private Integer stampHeight;
    private int pageNumber;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }

    public Integer getStampHeight() {
        return stampHeight;
    }

    public void setStampHeight(Integer stampHeight) {
        this.stampHeight = stampHeight;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Operation operation1 = openAPI.paths?.get("/path1")?.post
        Operation operation2 = openAPI.paths?.get("/path2")?.post

        expect:

        operation1
        operation1.externalDocs
        operation1.externalDocs.description == 'operation external docs'
        operation1.externalDocs.url == 'http://externalUrl'
        operation1.externalDocs.extensions.'x-myExt1'.prop1 == "prop1Val"
        operation1.externalDocs.extensions.'x-myExt1'.prop2 == "prop2Val"
        operation1.externalDocs.extensions.'x-myExt2'.prop1 == "prop1Val1"
        operation1.externalDocs.extensions.'x-myExt2'.prop2 == "prop2Val2"

        operation2
        operation2.externalDocs
        operation2.externalDocs.description == 'operation external docs'
        operation2.externalDocs.url == 'http://externalUrl'
        operation2.externalDocs.extensions.'x-myExt1'.prop1 == "prop1Val"
        operation2.externalDocs.extensions.'x-myExt1'.prop2 == "prop2Val"
        operation2.externalDocs.extensions.'x-myExt2'.prop1 == "prop1Val1"
        operation2.externalDocs.extensions.'x-myExt2'.prop2 == "prop2Val2"
    }

    void "test OpenAPI ExternalDocs on schema class level"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;

@Controller
class MyController {

    @Post("path")
    public void processSync(@Body MyDto myDto) {
    }
}

@ExternalDocumentation(
        description = "dto external docs",
        url = "http://externalUrl",
        extensions = {
                @Extension(
                        name = "myExt1",
                        properties = {
                                @ExtensionProperty(name = "prop1", value = "prop1Val"),
                                @ExtensionProperty(name = "prop2", value = "prop2Val"),
                        }
                ),
                @Extension(
                        name = "myExt2",
                        properties = {
                                @ExtensionProperty(name = "prop1", value = "prop1Val1"),
                                @ExtensionProperty(name = "prop2", value = "prop2Val2"),
                        }
                ),
        }
)
class MyDto {

    private Parameters parameters;

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

}

class Parameters {

    private Integer stampWidth;
    private Integer stampHeight;
    private int pageNumber;

    public Integer getStampWidth() {
        return stampWidth;
    }

    public void setStampWidth(Integer stampWidth) {
        this.stampWidth = stampWidth;
    }

    public Integer getStampHeight() {
        return stampHeight;
    }

    public void setStampHeight(Integer stampHeight) {
        this.stampHeight = stampHeight;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Schema schema = openAPI.components?.schemas?.MyDto

        expect:

        schema
        schema.externalDocs
        schema.externalDocs.description == 'dto external docs'
        schema.externalDocs.url == 'http://externalUrl'
    }
}
