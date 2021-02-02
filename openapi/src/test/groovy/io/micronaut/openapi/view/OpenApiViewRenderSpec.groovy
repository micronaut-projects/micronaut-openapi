package io.micronaut.openapi.view

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import io.micronaut.openapi.view.OpenApiViewConfig
import io.micronaut.openapi.visitor.OpenApiApplicationVisitor
import spock.lang.Specification

class OpenApiOperationViewRenderSpec extends Specification {
    def cleanup() {
        def outputDir = new File("output")
        outputDir.deleteDir()
    }

    void "test render OpenApiView specification"() {
        given:
        String spec = "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.specURL == "/swagger/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
    }

    void "test render OpenApiView specification with server context path"() {
        given:
        String spec = "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.serverContextPath = "/context-path"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.specURL == "/context-path/swagger/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
    }

    void "test render OpenApiView specification custom mapping path"() {
        given:
        String spec = "mapping.path=somewhere,redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "somewhere"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.specURL == "/somewhere/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
    }

    void "test render OpenApiView specification with custom mapping path and server context path"() {
        given:
        String spec = "mapping.path=somewhere,redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.serverContextPath = "/context-path"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "somewhere"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.specURL == "/context-path/somewhere/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        Files.notExists(outputDir.resolve("swagger-ui").resolve("oauth2-redirect.html"))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL())
        !outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("ui.initOAuth({")
    }

    void "test generates oauth2-redirect.html"() {
        given:
        String spec = "swagger-ui.enabled=true,swagger-ui.oauth2RedirectUrl=http://localhost:8080/foo/bar,swagger-ui.oauth2.clientId=foo,swagger-ui.oauth2.clientSecret=bar"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled == true
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"

        and:
        Path index = outputDir.resolve("swagger-ui").resolve("index.html")
        Path oauth2Redirect = outputDir.resolve("swagger-ui").resolve("oauth2-redirect.html")
        Files.exists(index)
        Files.exists(oauth2Redirect)

        and:
        String indexHtml = index.toFile().getText(StandardCharsets.UTF_8.name())
        indexHtml.contains(cfg.getSpecURL())
        indexHtml.contains("ui.initOAuth({")
        indexHtml.contains('clientId: "foo"')
        indexHtml.contains('clientSecret: "bar"')
    }

}
