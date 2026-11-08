package io.micronaut.openapi.visitor

import io.micronaut.inject.visitor.TypeElementVisitor
import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import spock.lang.Requires

/**
 * Two compilations in a row with the same classes of the visitors, as in a compiler daemon or in development mode, which
 * keep the processors' class loader: the second document holds what the second compilation has, nothing of the first.
 */
@Requires({ TypeElementVisitor.methods*.name.contains('finishCompilation') })
class OpenApiCompilationStateSpec extends AbstractOpenApiTypeElementSpec {

    void "a class renamed with the name of its schema keeps that name in the next compilation"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller("/pets")
class PetController {

    @Get
    Pet get() {
        return null;
    }
}

@Schema(name = "Pet")
class Pet {

    public String name;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference.components.schemas.keySet() == ['Pet'] as Set

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller("/pets")
class PetController {

    @Get
    Animal get() {
        return null;
    }
}

@Schema(name = "Pet")
class Animal {

    public String name;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference.components.schemas.keySet() == ['Pet'] as Set
        Utils.testReference.paths.'/pets'.get.responses.'200'.content.'application/json'.schema.$ref == '#/components/schemas/Pet'
    }

    void "an extra schema renamed is not in the next document under its old name"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.annotation.OpenAPIExtraSchema;

@Controller("/pets")
class PetController {

    @Get
    String get() {
        return null;
    }
}

@OpenAPIExtraSchema
class Pet {

    public String name;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference.components.schemas.keySet() == ['Pet'] as Set

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.annotation.OpenAPIExtraSchema;

@Controller("/pets")
class PetController {

    @Get
    String get() {
        return null;
    }
}

@OpenAPIExtraSchema
class Animal {

    public String name;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference.components.schemas.keySet() == ['Animal'] as Set
    }

    void "a schema removed is not in the next document"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.openapi.annotation.OpenAPIExtraSchema;

@Controller("/pets")
class PetController {

    @Get
    Pet get() {
        return null;
    }
}

class Pet {

    public String name;
}

@OpenAPIExtraSchema
class Owner {

    public String name;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference.components.schemas.keySet() == ['Owner', 'Pet'] as Set

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/pets")
class PetController {

    @Get
    Pet get() {
        return null;
    }
}

class Pet {

    public String name;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.testReference.components.schemas.keySet() == ['Pet'] as Set
    }
}
