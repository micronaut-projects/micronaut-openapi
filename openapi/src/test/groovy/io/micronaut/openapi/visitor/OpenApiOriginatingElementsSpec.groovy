package io.micronaut.openapi.visitor

import io.micronaut.annotation.processing.AggregatingTypeElementVisitorProcessor
import io.micronaut.annotation.processing.TypeElementVisitorProcessor
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir

import javax.tools.FileObject
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaFileManager
import javax.tools.StandardJavaFileManager
import javax.tools.StandardLocation
import javax.tools.ToolProvider
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * The elements the document is written for, which a compiler that compiles part of the sources again after an edit,
 * as Micronaut's development mode does, reads to know which sources write the document again: those whose
 * declarations the document describes, and no other.
 */
@Requires({ jvm.isJava18Compatible() })
class OpenApiOriginatingElementsSpec extends Specification {

    @TempDir
    Path dir

    def setup() {
        Utils.clean()
        System.clearProperty(Utils.ATTR_TEST_MODE)
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_ENABLED, "false")
    }

    def cleanup() {
        Utils.clean()
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_ENABLED)
    }

    void "the document is written for the classes it describes, not for every class with an annotation"() {
        when:
        def credits = compile(SOURCES, dir.resolve("classes"))

        then:
        credits.keySet() == ['META-INF/swagger/demo-1.yml'] as Set

        when:
        def document = credits['META-INF/swagger/demo-1.yml']

        then:
        // Application has @OpenAPIDefinition, Pet is a schema without an annotation and BaseDto its superclass,
        // PetApi, the superclass of PetController, declares its operation, HiddenController is a controller hidden for now
        // and StatusApi, the superclass of StatusController, declares an operation hidden for now;
        // PetService, a bean, and Owner, an introspected class no endpoint returns, are not described
        document == ['Application.java', 'BaseDto.java', 'HiddenController.java', 'Pet.java', 'PetApi.java', 'PetController.java', 'StatusApi.java', 'StatusController.java'] as Set
    }

    void "a compilation of classes the document does not describe writes no document"() {
        given:
        def classes = dir.resolve("classes")
        compile(SOURCES, classes)
        Utils.clean()

        when:
        def credits = compile(SOURCES.subMap(['test/PetService.java']), dir.resolve("again"), classes)

        then:
        credits.isEmpty()
    }

    private static final Map<String, String> SOURCES = [
        'test/Application.java': '''
package test;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(info = @Info(title = "demo", version = "1"))
public class Application {
}
''',
        'test/PetApi.java': '''
package test;

import io.micronaut.http.annotation.Get;

public abstract class PetApi {

    /**
     * Finds a pet.
     *
     * @param name The name
     * @return The pet
     */
    @Get("/{name}")
    public Pet find(String name) {
        return service().find(name);
    }

    protected abstract PetService service();
}
''',
        'test/PetController.java': '''
package test;

import io.micronaut.http.annotation.Controller;

@Controller("/pets")
public class PetController extends PetApi {

    private final PetService service;

    public PetController(PetService service) {
        this.service = service;
    }

    @Override
    protected PetService service() {
        return service;
    }
}
''',
        'test/HiddenController.java': '''
package test;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@Controller("/hidden")
public class HiddenController {

    @Get
    public String get() {
        return "";
    }
}
''',
        'test/StatusApi.java': '''
package test;

import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Hidden;

public abstract class StatusApi {

    @Hidden
    @Get
    public String status() {
        return "";
    }
}
''',
        'test/StatusController.java': '''
package test;

import io.micronaut.http.annotation.Controller;

@Controller("/status")
public class StatusController extends StatusApi {
}
''',
        'test/BaseDto.java': '''
package test;

public class BaseDto {

    /**
     * The identifier.
     */
    public long id;
}
''',
        'test/Pet.java': '''
package test;

/**
 * A pet.
 */
public class Pet extends BaseDto {

    /**
     * The name.
     */
    public String name;
}
''',
        'test/PetService.java': '''
package test;

import jakarta.inject.Singleton;

@Singleton
public class PetService {

    public Pet find(String name) {
        return null;
    }
}
''',
        'test/Owner.java': '''
package test;

import io.micronaut.core.annotation.Introspected;

/**
 * An owner no endpoint returns.
 */
@Introspected
public class Owner {

    public String name;
}
''',
    ]

    /**
     * Compiles the sources with the visitors and returns the resources written into the class output, each with the
     * names of the files they were written for.
     */
    private Map<String, Set<String>> compile(Map<String, String> sources, Path classes, Path... classpath) {
        def src = dir.resolve("src-" + classes.fileName)
        def files = sources.collect { name, text ->
            def file = src.resolve(name)
            Files.createDirectories(file.parent)
            Files.writeString(file, text)
            file
        }
        Files.createDirectories(classes)
        def javac = ToolProvider.systemJavaCompiler
        def standard = javac.getStandardFileManager(null, null, StandardCharsets.UTF_8)
        def recording = new RecordingFileManager(standard)
        standard.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, [classes])
        standard.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, [classes])
        def path = System.getProperty("java.class.path").split(File.pathSeparator).collect { Path.of(it) }
        standard.setLocationFromPaths(StandardLocation.CLASS_PATH, path + (classpath as List))
        def diagnostics = []
        def task = javac.getTask(null, recording, { diagnostics << it }, ['-parameters'], null, standard.getJavaFileObjectsFromPaths(files))
        task.setProcessors([new TypeElementVisitorProcessor(), new AggregatingTypeElementVisitorProcessor()])
        assert task.call(): diagnostics.join('\n')
        recording.credits
    }

    static class RecordingFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

        final Map<String, Set<String>> credits = [:]

        RecordingFileManager(StandardJavaFileManager fileManager) {
            super(fileManager)
        }

        // declared by JavaFileManager from Java 18, which javac calls with the originating elements' files
        FileObject getFileForOutputForOriginatingFiles(JavaFileManager.Location location, String packageName, String relativeName, FileObject... originatingFiles) {
            if (location == StandardLocation.CLASS_OUTPUT && relativeName.startsWith('META-INF/swagger/')) {
                credits.computeIfAbsent(relativeName) { new TreeSet<String>() }.addAll(originatingFiles.collect { Path.of(it.toUri()).fileName.toString() })
            }
            fileManager.getFileForOutput(location, packageName, relativeName, originatingFiles.length > 0 ? originatingFiles[0] : null)
        }
    }
}
