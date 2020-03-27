/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.view

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import io.micronaut.context.ApplicationContext
import io.micronaut.openapi.view.OpenApiViewConfig
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.Ignore
import spock.lang.Specification

class ServingOpenApiView extends Specification {
    def cleanup() {
        def outputDir = new File("output")
        outputDir.deleteDir()
    }

    void "serving OpenApiView"() {
        given:
        EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer, "openapi")
        String spec = "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "petstore.yml"
        cfg.render(outputDir, null)
        String contextPath = embeddedServer.environment.getProperty("micronaut.server.context-path", String, '')

        expect:
        contextPath == ''
        cfg.enabled == true
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "petstore.yml"
        cfg.specURL == "swagger/petstore.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))

        // views
        when:
        def url = new URL(embeddedServer.getURL().toString() + "/swagger-ui")
        def connection = url.openConnection()
        connection.requestMethod = 'GET'

        then:
        connection.responseCode == 200

        when:
        url = new URL(embeddedServer.getURL().toString() + "/redoc")
        connection = url.openConnection()
        connection.requestMethod = 'GET'

        then:
        connection.responseCode == 200

        when:
        url = new URL(embeddedServer.getURL().toString() + "/rapidoc")
        connection = url.openConnection()
        connection.requestMethod = 'GET'

        then:
        connection.responseCode == 200

        // spec file
        when:
        url = new URL(embeddedServer.getURL().toString() + '/' + cfg.getSpecURL())
        connection = url.openConnection()
        connection.requestMethod = 'GET'

        then:
        connection.responseCode == 200

        cleanup:
        embeddedServer?.close()
    }

    // Does not work with micronaut 1.1.x
    // https://github.com/micronaut-projects/micronaut-core/issues/2436
    @Ignore
    void "serving OpenApiView with context path"() {
        given:
        EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer, "openapicontextpath")
        String spec = "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "petstore.yml"
        cfg.render(outputDir, null)
        String contextPath = embeddedServer.environment.getProperty("micronaut.server.context-path", String, '')

        expect:
        contextPath == '/context-path'
        cfg.enabled == true
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "petstore.yml"
        cfg.specURL == "swagger/petstore.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))

        // views
        when:
        def url = new URL(embeddedServer.getURL().toString() + contextPath + "/swagger-ui")
        def connection = url.openConnection()
        connection.requestMethod = 'GET'

        then:
        connection.responseCode == 200

        when:
        url = new URL(embeddedServer.getURL().toString() + contextPath + "/redoc")
        connection = url.openConnection()
        connection.requestMethod = 'GET'

        then:
        connection.responseCode == 200

        when:
        url = new URL(embeddedServer.getURL().toString() + contextPath + "/rapidoc")
        connection = url.openConnection()
        connection.requestMethod = 'GET'

        then:
        connection.responseCode == 200

        // spec file
        when:
        url = new URL(embeddedServer.getURL().toString() + contextPath + '/' + cfg.getSpecURL())
        connection = url.openConnection()
        connection.requestMethod = 'GET'

        then:
        connection.responseCode == 200

        cleanup:
        embeddedServer?.close()
    }
}
