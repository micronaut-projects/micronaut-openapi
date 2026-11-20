package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.micronaut.openapi.OpenApiUtils
import io.micronaut.openapi.jackson.JacksonOpenApiMapperProvider
import io.micronaut.openapi.mapper.CountingOpenApiMapperProvider
import io.micronaut.openapi.mapper.OpenApiMapperProviders

import java.nio.file.Files
import java.nio.file.Path

class OpenApiMapperProviderSpec extends AbstractOpenApiTypeElementSpec {

    void cleanup() {
        System.clearProperty(OpenApiMapperProviders.MAPPER_PROPERTY)
    }

    void "the visitor uses the provider selected with micronaut.openapi.mapper"() {
        given:
        System.setProperty(OpenApiMapperProviders.MAPPER_PROPERTY, CountingOpenApiMapperProvider.NAME)
        CountingOpenApiMapperProvider.MAPPER_REQUESTS.set(0)

        when:
        buildBeanDefinition('test.MyBean', '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Controller
class PetController {

    @Get("/pet")
    @io.swagger.v3.oas.annotations.Operation(extensions = @Extension(name = "x-a", properties = @ExtensionProperty(name = "b", value = "c")))
    Pet get() {
        return null;
    }
}

class Pet {

    @Schema(example = "rex", allowableValues = {"rex", "fido"})
    public String name;
}

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        Utils.getMapperProvider() instanceof CountingOpenApiMapperProvider
        CountingOpenApiMapperProvider.MAPPER_REQUESTS.get() > 0
        Utils.testReference.paths."/pet".get.extensions == ['x-a': [b: 'c']]
        Utils.testReference.components.schemas.Pet.properties.name.example == 'rex'
        Utils.testReference.components.schemas.Pet.properties.name.enum == ['rex', 'fido']
    }

    void "jackson is preferred when several providers are available and none is selected"() {
        when:
        buildBeanDefinition('test.MyBean', '''
package test;

@jakarta.inject.Singleton
class MyBean {}
''')

        then:
        OpenApiMapperProviders.findAll(Utils.classLoader)*.name == ['counting', 'jackson']
        Utils.getMapperProvider() instanceof JacksonOpenApiMapperProvider
    }

    void "the visitor does not reference Jackson databind or micronaut-openapi-jackson classes"() {
        given:
        Path visitorClasses = Path.of(Utils.protectionDomain.codeSource.location.toURI())
        Path jacksonModuleClasses = Path.of(OpenApiUtils.protectionDomain.codeSource.location.toURI())
        List<String> forbidden = ['tools/jackson/databind/', 'tools/jackson/core/', 'tools/jackson/dataformat/'] +
            classNames(jacksonModuleClasses)

        when:
        Map<String, List<String>> offending = [:]
        classFiles(visitorClasses).each { Path classFile ->
            String content = new String(Files.readAllBytes(classFile), 'ISO-8859-1')
            List<String> found = forbidden.findAll { content.contains(it) }
            if (found) {
                offending[visitorClasses.relativize(classFile).toString()] = found
            }
        }

        then:
        !classNames(jacksonModuleClasses).isEmpty()
        offending.isEmpty()
    }

    private static List<Path> classFiles(Path root) {
        if (Files.isDirectory(root)) {
            return Files.walk(root).withCloseable { it.filter { it.toString().endsWith('.class') }.toList() }
        }
        def fs = java.nio.file.FileSystems.newFileSystem(root)
        return Files.walk(fs.getPath('/')).withCloseable { it.filter { it.toString().endsWith('.class') }.toList() }
    }

    /**
     * Internal names of the classes in micronaut-openapi-jackson, such as {@code io/micronaut/openapi/OpenApiUtils}.
     */
    private static List<String> classNames(Path root) {
        classFiles(root).collect { Path classFile ->
            String name = (Files.isDirectory(root) ? root.relativize(classFile) : classFile).toString()
            name = name.startsWith('/') ? name.substring(1) : name
            // match "io/micronaut/openapi/OpenApiUtils;" and "io/micronaut/openapi/OpenApiUtils$..." references
            name.replace(File.separator, '/').replaceFirst(/\.class$/, '').replaceFirst(/\$.*/, '')
        }.unique()
    }
}
