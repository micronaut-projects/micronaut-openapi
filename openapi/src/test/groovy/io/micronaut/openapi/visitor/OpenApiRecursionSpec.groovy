package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.RequestBody

class OpenApiRecursionSpec extends AbstractTypeElementSpec {

    def setup() {
        System.setProperty(AbstractOpenApiVisitor.ATTR_TEST_MODE, "true")
    }

    void "test OpenAPI handles recursion"() {
        given:
            buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import io.micronaut.http.annotation.*;
import io.reactivex.Maybe;

@Controller("/")
class MyController {
    @Get
    public Maybe<TestInterface> hey() {
        return Maybe.empty();
    }
}

@Schema(oneOf = {TestImpl1.class, TestImpl2.class})
interface TestInterface {
    String getType();
}

class TestImpl1 implements TestInterface {

    private TestInterface woopsie;

    @Override
    public String getType() {
        return null;
    }

    public TestInterface getWoopsie() {
        return woopsie;
    }

    public void setWoopsie(TestInterface woopsie) {
        this.woopsie = woopsie;
    }
}

class TestImpl2 implements TestInterface {
    @Override
    public String getType() {
        return null;
    }
}

@javax.inject.Singleton
class MyBean {}
''')

            OpenAPI openAPI = AbstractOpenApiVisitor.testReference
            Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
            Schema testImpl1 = schemas.get("TestImpl1")
            Schema woopsieRef = testImpl1.getProperties()["woopsie"]
            woopsieRef
            woopsieRef.$ref == "#/components/schemas/TestInterface"
    }

    void "test OpenAPI handles recursion when no @Schema annotation"() {
        given:
            buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import io.micronaut.http.annotation.*;

@Controller("/")
class MyController {
    @Get
    public TestRecursion hey() {
        return null;
    }
}

class TestRecursion {

    private TestRecursion woopsie;

    public TestRecursion getWoopsie() {
        return woopsie;
    }

    public void setWoopsie(TestRecursion woopsie) {
        this.woopsie = woopsie;
    }
}

@javax.inject.Singleton
class MyBean {}
''')

            OpenAPI openAPI = AbstractOpenApiVisitor.testReference
            Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
            Schema testRecursion = schemas.get("TestRecursion")
            Schema woopsieRef = testRecursion.getProperties()["woopsie"]
            woopsieRef
            woopsieRef.$ref == "#/components/schemas/TestRecursion"
    }

    void "test OpenAPI handles recursion when recursive item has different name"() {
        given:
            buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import io.micronaut.http.annotation.*;
import io.reactivex.Maybe;

@Controller("/")
class MyController {
    @Get
    public Maybe<TestInterface> hey() {
        return Maybe.empty();
    }
}

@Schema(oneOf = {TestImpl1.class, TestImpl2.class})
interface TestInterface {
    String getType();
}

class TestImpl1 implements TestInterface {

    private TestInterface woopsie;

    @Override
    public String getType() {
        return null;
    }

    @Schema(name = "woopsie-id", description = "woopsie doopsie", oneOf = {TestImpl1.class, TestImpl2.class})
    public TestInterface getWoopsie() {
        return woopsie;
    }

    public void setWoopsie(TestInterface woopsie) {
        this.woopsie = woopsie;
    }
}

class TestImpl2 implements TestInterface {
    @Override
    public String getType() {
        return null;
    }
}

@javax.inject.Singleton
class MyBean {}
''')

            OpenAPI openAPI = AbstractOpenApiVisitor.testReference
            Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
            Schema testImpl1 = schemas.get("TestImpl1")
            Schema woopsieRef = testImpl1.getProperties()["woopsie"]
            woopsieRef
            woopsieRef.$ref == "#/components/schemas/woopsie-id"
            Schema woopsie = schemas.get("woopsie-id")
            woopsie.description == "woopsie doopsie"
    }

    void "test OpenAPI applies additional annotations to recursive property"() {
        given:
            buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import io.reactivex.Maybe;

@Controller("/")
class MyController {
    @Get
    public Maybe<TestInterface> hey() {
        return Maybe.empty();
    }
}

@Schema(oneOf = {TestImpl1.class, TestImpl2.class})
interface TestInterface {
    String getType();
}

class TestImpl1 implements TestInterface {

    private TestInterface woopsie;

    @Override
    public String getType() {
        return null;
    }
    
    /**
     * Some docs
     */
    @Nullable
    @Deprecated
    public TestInterface getWoopsie() {
        return woopsie;
    }

    public void setWoopsie(TestInterface woopsie) {
        this.woopsie = woopsie;
    }
}

class TestImpl2 implements TestInterface {
    @Override
    public String getType() {
        return null;
    }
}

@javax.inject.Singleton
class MyBean {}
''')

            OpenAPI openAPI = AbstractOpenApiVisitor.testReference
            Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
            Schema testImpl1 = schemas.get("TestImpl1")
            Schema woopsieProperty = testImpl1.getProperties()["woopsie"]
            woopsieProperty instanceof ComposedSchema
            ((ComposedSchema) woopsieProperty).allOf[0].$ref == "#/components/schemas/TestInterface"
            ((ComposedSchema) woopsieProperty).allOf[1].deprecated
            ((ComposedSchema) woopsieProperty).allOf[1].description == "Some docs"
            ((ComposedSchema) woopsieProperty).allOf[1].nullable
    }

}
