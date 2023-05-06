package io.micronaut.openapi.visitor

import io.micronaut.context.env.Environment
import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class OpenapiCustomSchemaSpec extends AbstractOpenApiTypeElementSpec {

    void "test custom OpenAPI schema for class"() {
        given:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE, "openapi-custom-schema-for-class.properties")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.openapi.JAXBElement;
import io.micronaut.openapi.ObjectId;

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

class MyDto {

    private ObjectId id;
    private JAXBElement<? extends XmlElement> xmlElement;
    private JAXBElement<? extends XmlElement2> xmlElement2;
    public JAXBElement<? extends XmlElement3> xmlElement3;
    public JAXBElement<? extends XmlElement4> xmlElement4;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public JAXBElement<? extends XmlElement> getXmlElement() {
        return xmlElement;
    }

    public void setXmlElement(JAXBElement<? extends XmlElement> xmlElement) {
        this.xmlElement = xmlElement;
    }

    public JAXBElement<? extends XmlElement2> getXmlElement2() {
        return xmlElement2;
    }

    public void setXmlElement2(JAXBElement<? extends XmlElement2> xmlElement2) {
        this.xmlElement2 = xmlElement2;
    }
}

class XmlElement {

    private String propStr;
    private int propNumPrimitive;
    public Integer propNum;

    public String getPropStr() {
        return propStr;
    }

    public void setPropStr(String propStr) {
        this.propStr = propStr;
    }

    public int getPropNumPrimitive() {
        return propNumPrimitive;
    }

    public void setPropNumPrimitive(int propNumPrimitive) {
        this.propNumPrimitive = propNumPrimitive;
    }
}

class XmlElement2 {

    private String propStr2;

    public String getPropStr2() {
        return propStr2;
    }

    public void setPropStr2(String propStr2) {
        this.propStr2 = propStr2;
    }
}

class XmlElement3 {

    private String propStr3;

    public String getPropStr3() {
        return propStr3;
    }

    public void setPropStr3(String propStr3) {
        this.propStr3 = propStr3;
    }
}

class XmlElement4 {

    private String propStr3;

    public String getPropStr3() {
        return propStr3;
    }

    public void setPropStr3(String propStr3) {
        this.propStr3 = propStr3;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        Schema myDtoSchema = openAPI.components.schemas.MyDto
        Schema myJaxbElementSchema = openAPI.components.schemas.MyJaxbElement_XmlElement_
        Schema myJaxbElement2Schema = openAPI.components.schemas.MyJaxbElement2
        Schema myJaxbElement3Schema = openAPI.components.schemas.MyJaxbElement3
        Schema myJaxbElement4Schema = openAPI.components.schemas.MyJaxbElement4
        Schema discountSchema = openAPI.components.schemas."MyJaxbElement4.Discount"
        Schema xmlElementSchema = openAPI.components.schemas.XmlElement

        then:

        myDtoSchema
        myDtoSchema.properties.id
        myDtoSchema.properties.id.type == 'string'
        myDtoSchema.properties.xmlElement.$ref == '#/components/schemas/MyJaxbElement_XmlElement_'
        myDtoSchema.properties.xmlElement2.$ref == '#/components/schemas/MyJaxbElement2'
        myDtoSchema.properties.xmlElement3.$ref == '#/components/schemas/MyJaxbElement3'

        myJaxbElementSchema
        myJaxbElementSchema.properties.type.type == 'string'
        myJaxbElementSchema.properties.value.$ref == '#/components/schemas/XmlElement'

        myJaxbElement2Schema
        myJaxbElement2Schema.properties.type.type == 'string'
        myJaxbElement2Schema.properties.values.type == 'array'
        myJaxbElement2Schema.properties.values.items.type == 'string'

        myJaxbElement3Schema
        myJaxbElement3Schema.properties.type.type == 'string'
        myJaxbElement3Schema.properties.value.type == 'string'

        myJaxbElement4Schema
        myJaxbElement4Schema.properties.type.$ref
        myJaxbElement4Schema.properties.type.$ref == '#/components/schemas/MyJaxbElement4.DiscountTypeType'

        myJaxbElement4Schema.properties.value.allOf
        myJaxbElement4Schema.properties.value.allOf.size() == 2
        myJaxbElement4Schema.properties.value.allOf.get(0).$ref == '#/components/schemas/MyJaxbElement4.Discount'
        myJaxbElement4Schema.properties.value.allOf.get(1).oneOf
        myJaxbElement4Schema.properties.value.allOf.get(1).oneOf.size() == 3
        myJaxbElement4Schema.properties.value.allOf.get(1).oneOf.get(0).$ref == '#/components/schemas/MyJaxbElement4.DiscountSizeOpenApi'
        myJaxbElement4Schema.properties.value.allOf.get(1).oneOf.get(1).$ref == '#/components/schemas/MyJaxbElement4.DiscountFixedOpenApi'
        myJaxbElement4Schema.properties.value.allOf.get(1).oneOf.get(2).$ref == '#/components/schemas/MyJaxbElement4.MultiplierSizeOpenApi'

        xmlElementSchema
        xmlElementSchema.properties.propStr.type == 'string'
        xmlElementSchema.properties.propNumPrimitive.type == 'integer'
        xmlElementSchema.properties.propNum.type == 'integer'

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONFIG_FILE)
    }

    void "test custom OpenAPI schema for class with env properties"() {
        given:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS, "project:/src/test/resources/")
        System.setProperty(Environment.ENVIRONMENTS_PROPERTY, "schemaforclass")

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.openapi.JAXBElement;
import io.micronaut.openapi.ObjectId;

@Controller
class OpenApiController {

    @Post("/path")
    public void processSync(@Body MyDto dto) {
    }
}

class MyDto {

    private ObjectId id;
    private JAXBElement<? extends XmlElement> xmlElement;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public JAXBElement<? extends XmlElement> getXmlElement() {
        return xmlElement;
    }

    public void setXmlElement(JAXBElement<? extends XmlElement> xmlElement) {
        this.xmlElement = xmlElement;
    }
}

class XmlElement {

    private String propStr;
    private int propNumPrimitive;
    public Integer propNum;

    public String getPropStr() {
        return propStr;
    }

    public void setPropStr(String propStr) {
        this.propStr = propStr;
    }

    public int getPropNumPrimitive() {
        return propNumPrimitive;
    }

    public void setPropNumPrimitive(int propNumPrimitive) {
        this.propNumPrimitive = propNumPrimitive;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')
        then:
        Utils.testReference != null

        when:
        OpenAPI openAPI = Utils.testReference
        Schema myDtoSchema = openAPI.components.schemas.MyDto
        Schema myJaxbElementSchema = openAPI.components.schemas.MyJaxbElement_XmlElement_
        Schema xmlElementSchema = openAPI.components.schemas.XmlElement

        then:

        myDtoSchema
        myDtoSchema.properties.id
        myDtoSchema.properties.id.type == 'string'
        myDtoSchema.properties.xmlElement.$ref == '#/components/schemas/MyJaxbElement_XmlElement_'

        myJaxbElementSchema
        myJaxbElementSchema.properties.type.type == 'string'
        myJaxbElementSchema.properties.value.$ref == '#/components/schemas/XmlElement'

        xmlElementSchema
        xmlElementSchema.properties.propStr.type == 'string'
        xmlElementSchema.properties.propNumPrimitive.type == 'integer'
        xmlElementSchema.properties.propNum.type == 'integer'

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_CONFIG_FILE_LOCATIONS)
        System.clearProperty(Environment.ENVIRONMENTS_PROPERTY)
    }
}
