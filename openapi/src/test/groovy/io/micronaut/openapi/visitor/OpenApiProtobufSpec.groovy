package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec

class OpenApiProtobufSpec extends AbstractOpenApiTypeElementSpec {

    void "test protobuf parameters"() {

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.openapi.proto.ProductsListProto;

@Controller
class ControllerThree {

    @Post("/myObj")
    MyObj myMethod(@Body ProductsListProto productsListProto) {
        return null;
    }
}

class MyObj {

    public String myProp;
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference

        when:
        def openApi = Utils.testReference
        def subObject = openApi.components.schemas.SubObjectProto
        def product = openApi.components.schemas.ProductProto
        def productsList = openApi.components.schemas.ProductsListProto
        def myEnum = openApi.components.schemas.MyEnum

        then:
        subObject
        subObject.properties.size() == 1
        subObject.properties.reqField.type == 'integer'

        product
        product.properties.domain.type == 'integer'
        product.properties.uintField.type == 'integer'
        product.properties.int32.type == 'integer'
        product.properties.int64.type == 'integer'
        product.properties.enumVal.$ref == "#/components/schemas/MyEnum"

        productsList
        productsList.properties.size() == 1
        productsList.properties.products.type == 'array'
        productsList.properties.products.items.$ref == "#/components/schemas/ProductProto"

        myEnum.type == 'integer'
        myEnum.enum.size() == 2
        myEnum.enum[0] == 0
        myEnum.enum[1] == 1
    }
}
