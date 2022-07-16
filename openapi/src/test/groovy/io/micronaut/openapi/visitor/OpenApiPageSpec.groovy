package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema

class OpenApiPageSpec extends AbstractOpenApiTypeElementSpec {

    void "test openAPI micronaut data page"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller
class MyController {

    @Post
    public Page<MyDto> getSomeDTOs(@Body Pageable pageable) {
        return null;
    }
}

class MyDto {

    private String parameters;

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        Schema myDtoSchema = openAPI.components.schemas['MyDto']
        Schema pageMyDtoSchema = openAPI.components.schemas['Page_MyDto_']
        Schema sliceMyDtoSchema = openAPI.components.schemas['Slice_MyDto_']
        Schema sortSchema = openAPI.components.schemas['Sort']
        Schema sortOrderSchema = openAPI.components.schemas['Sort.Order']
        Schema sortOrderDirectionSchema = openAPI.components.schemas['Sort.Order.Direction']

        then:
        myDtoSchema
        pageMyDtoSchema
        sliceMyDtoSchema
        sortSchema
        sortOrderSchema
        sortOrderDirectionSchema

        myDtoSchema.properties.parameters
        pageMyDtoSchema.allOf
        pageMyDtoSchema.allOf.size() == 2
        pageMyDtoSchema.allOf.get(0) instanceof Schema
        ((Schema)pageMyDtoSchema.allOf.get(0)).$ref == '#/components/schemas/Slice_MyDto_'
        pageMyDtoSchema.allOf.get(1) instanceof ObjectSchema
        ((Schema)pageMyDtoSchema.allOf.get(1)).properties.totalSize
        ((Schema)pageMyDtoSchema.allOf.get(1)).properties.totalPages

        sliceMyDtoSchema.properties.content
        sliceMyDtoSchema.properties.pageable
        sliceMyDtoSchema.properties.pageNumber
        sliceMyDtoSchema.properties.offset
        sliceMyDtoSchema.properties.size
        sliceMyDtoSchema.properties.empty
        sliceMyDtoSchema.properties.numberOfElements

        sortSchema.properties.sorted
        sortSchema.properties.orderBy

        sortOrderSchema.properties.ignoreCase
        sortOrderSchema.properties.direction
        sortOrderSchema.properties.property
        sortOrderSchema.properties.ascending

        sortOrderDirectionSchema.enum[0] == 'ASC'
        sortOrderDirectionSchema.enum[1] == 'DESC'
    }
}
