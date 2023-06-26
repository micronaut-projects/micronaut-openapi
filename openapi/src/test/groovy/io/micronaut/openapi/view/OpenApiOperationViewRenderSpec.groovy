package io.micronaut.openapi.view

import io.micronaut.openapi.visitor.OpenApiApplicationVisitor
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class OpenApiOperationViewRenderSpec extends Specification {
    def cleanup() {
        def outputDir = new File("output")
        outputDir.deleteDir()
    }

    void "test render OpenApiView specification"() {
        given:
        String spec = "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true,rapipdf.enabled=true,swagger-ui.theme=flattop"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
        cfg.mappingPath == "swagger"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.swaggerUIConfig, null) == "/swagger/swagger.yml"
        cfg.getSpecURL(cfg.rapidocConfig, null) == "/swagger/swagger.yml"
        cfg.getSpecURL(cfg.redocConfig, null) == "/swagger/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("redoc").resolve("res").resolve("redoc.standalone.js"))
        Files.exists(outputDir.resolve("redoc").resolve("res").resolve("rapipdf-min.js"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("res").resolve("rapidoc-min.js"))
        Files.exists(outputDir.resolve("rapidoc").resolve("res").resolve("rapipdf-min.js"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("swagger-ui.css"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("favicon-16x16.png"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("favicon-32x32.png"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("swagger-ui-bundle.js"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("swagger-ui-standalone-preset.js"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("rapipdf-min.js"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("flattop.css"))

        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/redoc/res/redoc.standalone.js")
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.redocConfig, null))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/redoc/res/rapipdf-min.js")
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/rapidoc/res/rapidoc-min.js")
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.rapidocConfig, null))
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/rapidoc/res/rapipdf-min.js")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/swagger-ui/res/swagger-ui-bundle.js")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/swagger-ui/res/swagger-ui-standalone-preset.js")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/swagger-ui/res/swagger-ui.css")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/swagger-ui/res/favicon-32x32.png")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/swagger-ui/res/favicon-16x16.png")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.swaggerUIConfig, null))
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/swagger-ui/res/rapipdf-min.js")
    }

    void "test render OpenApiView specification with custom redoc js url"() {
        given:
        String spec = "redoc.enabled=true,rapipdf.enabled=true,redoc.copy-resources=false,redoc.js.url=https://cdn.redoc.ly/redoc/latest/bundles/"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
        cfg.mappingPath == "swagger"
        cfg.rapidocConfig == null
        cfg.redocConfig != null
        cfg.swaggerUIConfig == null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.redocConfig, null) == "/swagger/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        !Files.exists(outputDir.resolve("redoc").resolve("res").resolve("redoc.standalone.js"))
        Files.exists(outputDir.resolve("redoc").resolve("res").resolve("rapipdf-min.js"))

        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("<script src='https://cdn.redoc.ly/redoc/latest/bundles/redoc.standalone.js'></script>")
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.redocConfig, null))
    }

    void "test render OpenApiView specification with custom swagger js and css urls"() {
        given:
        String spec = "swagger-ui.enabled=true,rapipdf.enabled=true,swagger-ui.theme=flattop,swagger-ui.copy-theme=false,swagger-ui.theme.url=https://flattop.com/theme.css,swagger-ui.copy-resources=false,swagger-ui.js.url=https://unpkg.com/swagger-ui-dist/"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
        cfg.mappingPath == "swagger"
        cfg.rapidocConfig == null
        cfg.redocConfig == null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.swaggerUIConfig, null) == "/swagger/swagger.yml"
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        !Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("swagger-ui.css"))
        !Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("favicon-16x16.png"))
        !Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("favicon-32x32.png"))
        !Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("swagger-ui-bundle.js"))
        !Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("swagger-ui-standalone-preset.js"))
        !Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("flattop.css"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("res").resolve("rapipdf-min.js"))

        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.swaggerUIConfig, null))
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("<script src='https://unpkg.com/swagger-ui-dist/swagger-ui-standalone-preset.js'></script>")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("<script src='https://unpkg.com/swagger-ui-dist/swagger-ui-bundle.js'></script>")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("<link rel='stylesheet' type='text/css' href='https://unpkg.com/swagger-ui-dist/swagger-ui.css' />")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("<link rel='stylesheet' type='text/css' href='https://flattop.com/theme.css' />")
    }

    void "test render OpenApiView specification with server context path"() {
        given:
        String spec = "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.serverContextPath = "/context-path"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
        cfg.mappingPath == "swagger"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.rapidocConfig, null) == "/context-path/swagger/swagger.yml"
        cfg.getSpecURL(cfg.redocConfig, null) == "/context-path/swagger/swagger.yml"
        cfg.getSpecURL(cfg.swaggerUIConfig, null) == "/context-path/swagger/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.redocConfig, null))
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.rapidocConfig, null))
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.swaggerUIConfig, null))
    }

    void "test render OpenApiView specification custom mapping path"() {
        given:
        String spec = "mapping.path=somewhere,redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
        cfg.mappingPath == "somewhere"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.rapidocConfig, null) == "/somewhere/swagger.yml"
        cfg.getSpecURL(cfg.redocConfig, null) == "/somewhere/swagger.yml"
        cfg.getSpecURL(cfg.swaggerUIConfig, null) == "/somewhere/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.redocConfig, null))
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.rapidocConfig, null))
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.swaggerUIConfig, null))
    }

    void "test render OpenApiView specification with custom mapping path and server context path"() {
        given:
        String spec = "mapping.path=somewhere,redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.serverContextPath = "/context-path"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
        cfg.mappingPath == "somewhere"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.rapidocConfig, null) == "/context-path/somewhere/swagger.yml"
        cfg.getSpecURL(cfg.redocConfig, null) == "/context-path/somewhere/swagger.yml"
        cfg.getSpecURL(cfg.swaggerUIConfig, null) == "/context-path/somewhere/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        Files.notExists(outputDir.resolve("swagger-ui").resolve("oauth2-redirect.html"))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.redocConfig, null))
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.rapidocConfig, null))
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.swaggerUIConfig, null))
        !outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("ui.initOAuth({")
    }

    void "test generates oauth2-redirect.html"() {
        given:
        String spec = "swagger-ui.enabled=true,swagger-ui.oauth2RedirectUrl=http://localhost:8080/foo/bar,swagger-ui.oauth2.clientId=foo,swagger-ui.oauth2.clientSecret=bar"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
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
        indexHtml.contains(cfg.getSpecURL(cfg.swaggerUIConfig, null))
        indexHtml.contains("ui.initOAuth({")
        indexHtml.contains('clientId: "foo"')
        indexHtml.contains('clientSecret: "bar"')
    }

    void "test generates urlResourcesPrefix context-path and openapi.context.path"() {
        given:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_SERVER_CONTEXT_PATH, "/local-path")
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, "/server-context-path")
        String spec = "swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"

        String urlPrefix = cfg.swaggerUIConfig.getFinalUrlPrefix(OpenApiViewConfig.RendererType.SWAGGER_UI, null)

        urlPrefix
        urlPrefix == '/server-context-path/local-path/swagger-ui/res/'

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_SERVER_CONTEXT_PATH)
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH)
    }

    void "test generates urlResourcesPrefix only context-path"() {
        given:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, "/server-context-path")
        String spec = "swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"

        String urlPrefix = cfg.swaggerUIConfig.getFinalUrlPrefix(OpenApiViewConfig.RendererType.SWAGGER_UI, null)

        urlPrefix
        urlPrefix == '/server-context-path/swagger-ui/res/'

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH)
    }

    void "test generates urlResourcesPrefix only openapi.context.path"() {
        given:
        System.setProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, "/server-context-path")
        String spec = "swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"

        String urlPrefix = cfg.swaggerUIConfig.getFinalUrlPrefix(OpenApiViewConfig.RendererType.SWAGGER_UI, null)

        urlPrefix
        urlPrefix == '/server-context-path/swagger-ui/res/'

        cleanup:
        System.clearProperty(OpenApiApplicationVisitor.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH)
    }

    void "test generates urlResourcesPrefix without context paths"() {
        given:
        String spec = "swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.render(outputDir, null)

        expect:
        cfg.enabled
        cfg.swaggerUIConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"

        String urlPrefix = cfg.swaggerUIConfig.getFinalUrlPrefix(OpenApiViewConfig.RendererType.SWAGGER_UI, null)

        urlPrefix
        urlPrefix == '/swagger-ui/res/'

    }
}
