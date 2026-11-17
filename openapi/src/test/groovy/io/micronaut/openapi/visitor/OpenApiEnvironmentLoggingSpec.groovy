package io.micronaut.openapi.visitor

import spock.lang.Specification

class OpenApiEnvironmentLoggingSpec extends Specification {
    void "creating the annotation processing environment does not initialize SLF4J"() {
        given:
        URL[] urls = ([getClass().getResource('/environment-logging/')] +
            System.getProperty('micronaut.openapi.test.processor.classpath')
                .split(File.pathSeparator)
                .collect { new File(it).toURI().toURL() }) as URL[]
        def isolated = new URLClassLoader(urls, ClassLoader.platformClassLoader) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name == 'org.slf4j.LoggerFactory') {
                    throw new AssertionError('OpenAPI environment attempted to load LoggerFactory')
                }
                return super.loadClass(name, resolve)
            }
        }

        when:
        isolated.loadClass(OpenApiEnvironmentLoggingProbe.name).getMethod('run').invoke(null)

        then:
        noExceptionThrown()

        cleanup:
        isolated.close()
    }
}
