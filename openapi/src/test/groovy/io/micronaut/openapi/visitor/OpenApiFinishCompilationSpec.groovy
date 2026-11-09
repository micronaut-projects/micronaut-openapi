package io.micronaut.openapi.visitor

import io.micronaut.inject.visitor.VisitorContext
import io.micronaut.openapi.AbstractOpenApiTypeElementSpec

import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ENABLED

/**
 * Calls {@link OpenApiApplicationVisitor#finishCompilation} directly, as Micronaut 5.3.0 and later do once the last round
 * is over, so that it runs against a core that never calls it as well.
 */
class OpenApiFinishCompilationSpec extends AbstractOpenApiTypeElementSpec {

    def cleanup() {
        System.clearProperty(MICRONAUT_OPENAPI_ENABLED)
    }

    void "the finish that follows finishCompilation releases the compilation's state and keeps the document"() {
        given: 'a compilation that leaves its schema names and extra schemas in static fields'
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
        def document = Utils.testReference
        def visitor = new OpenApiApplicationVisitor()
        // a disabled OpenAPI returns before the document is written again, so only the release is left to run
        def context = Stub(VisitorContext) {
            getOptions() >> [(MICRONAUT_OPENAPI_ENABLED): 'false']
        }

        expect:
        document.components.schemas.keySet() == ['Owner', 'Pet'] as Set
        SchemaDefinitionUtils.schemaNameToClassNameMap.containsKey('Pet')
        OpenApiExtraSchemaVisitor.extraSchemas.containsKey('Owner')

        when: 'a round finishes before the compilation does'
        visitor.finish(context)

        then: 'the state stays for the next round'
        SchemaDefinitionUtils.schemaNameToClassNameMap.containsKey('Pet')
        OpenApiExtraSchemaVisitor.extraSchemas.containsKey('Owner')

        when: 'the compilation finishes'
        visitor.finishCompilation(context)

        then: 'the state stays until the finish that follows'
        SchemaDefinitionUtils.schemaNameToClassNameMap.containsKey('Pet')
        OpenApiExtraSchemaVisitor.extraSchemas.containsKey('Owner')

        when:
        visitor.finish(context)

        then: 'the state is released, and the document just written can still be read'
        SchemaDefinitionUtils.schemaNameToClassNameMap.isEmpty()
        OpenApiExtraSchemaVisitor.extraSchemas.isEmpty()
        Utils.testReference.is(document)

        when: 'the state of a later compilation is not released by a round of it'
        OpenApiExtraSchemaVisitor.extraSchemas.put('Owner', document.components.schemas.Owner)
        visitor.finish(context)

        then:
        OpenApiExtraSchemaVisitor.extraSchemas.containsKey('Owner')
    }
}
