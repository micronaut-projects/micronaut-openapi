package io.micronaut.openapi.view

import io.micronaut.openapi.visitor.OpenApiConfigProperty
import io.micronaut.openapi.visitor.Pair
import io.micronaut.openapi.visitor.group.OpenApiInfo
import io.swagger.v3.oas.models.OpenAPI
import org.apache.groovy.util.Maps
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import spock.util.environment.RestoreSystemProperties

class OpenApiOperationViewRenderSpec extends Specification {
    def cleanup() {
        def outputDir = new File("output")
        outputDir.deleteDir()
    }

    void "test render OpenApiView specification"() {
        given:
        String spec = "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true,openapi-explorer.enabled=true,rapipdf.enabled=true,swagger-ui.theme=flattop"
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
        cfg.openApiExplorerConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.swaggerUIConfig, null) == "/swagger/swagger.yml"
        cfg.getSpecURL(cfg.rapidocConfig, null) == "/swagger/swagger.yml"
        cfg.getSpecURL(cfg.redocConfig, null) == "/swagger/swagger.yml"
        cfg.getSpecURL(cfg.openApiExplorerConfig, null) == "/swagger/swagger.yml"
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
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("bootstrap.min.css"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("default.min.css"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("font-awesome.min.css"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("highlight.min.js"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("openapi-explorer.min.js"))

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
        outputDir.resolve("openapi-explorer").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/openapi-explorer/res/bootstrap.min.css")
        outputDir.resolve("openapi-explorer").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/openapi-explorer/res/default.min.css")
        outputDir.resolve("openapi-explorer").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/openapi-explorer/res/font-awesome.min.css")
        outputDir.resolve("openapi-explorer").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("/openapi-explorer/res/openapi-explorer.min.js")
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
        cfg.openApiExplorerConfig == null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.redocConfig, null) == "/swagger/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        !Files.exists(outputDir.resolve("redoc").resolve("res").resolve("redoc.standalone.js"))
        Files.exists(outputDir.resolve("redoc").resolve("res").resolve("rapipdf-min.js"))

        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("script(contextPath + \"https://cdn.redoc.ly/redoc/latest/bundles/redoc.standalone.js\", head, true)")
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.redocConfig, null))
    }

    void "test render OpenApiView specification with custom openapi explorer js url"() {
        given:
        String spec = "openapi-explorer.enabled=true,rapipdf.enabled=true,openapi-explorer.copy-resources=false,openapi-explorer.js.url=https://unpkg.com/openapi-explorer/dist/browser/"
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
        cfg.swaggerUIConfig == null
        cfg.openApiExplorerConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.openApiExplorerConfig, null) == "/swagger/swagger.yml"
        Files.exists(outputDir.resolve("openapi-explorer").resolve("index.html"))
        !Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("openapi-explorer.min.js"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("res").resolve("rapipdf-min.js"))

        outputDir.resolve("openapi-explorer").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("script(contextPath + \"https://unpkg.com/openapi-explorer/dist/browser/openapi-explorer.min.js\", head, \"module\", true)")
        outputDir.resolve("openapi-explorer").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.openApiExplorerConfig, null))
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
        cfg.openApiExplorerConfig == null
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
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("script(contextPath + \"https://unpkg.com/swagger-ui-dist/swagger-ui-standalone-preset.js\", head)")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("script(contextPath + \"https://unpkg.com/swagger-ui-dist/swagger-ui-bundle.js\", head)")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("link(contextPath + \"https://unpkg.com/swagger-ui-dist/swagger-ui.css\", head, \"text/css\", \"stylesheet\")")
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains("link(contextPath + \"https://flattop.com/theme.css\", head, \"text/css\", \"stylesheet\")")
    }

    @RestoreSystemProperties
    void "test render OpenApiView specification with server context path"() {
        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, "/context-path")
        String spec = "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true,openapi-explorer.enabled=true"
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
        cfg.openApiExplorerConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.rapidocConfig, null) == "/context-path/swagger/swagger.yml"
        cfg.getSpecURL(cfg.redocConfig, null) == "/context-path/swagger/swagger.yml"
        cfg.getSpecURL(cfg.swaggerUIConfig, null) == "/context-path/swagger/swagger.yml"
        cfg.getSpecURL(cfg.openApiExplorerConfig, null) == "/context-path/swagger/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.redocConfig, null))
        outputDir.resolve("openapi-explorer").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.openApiExplorerConfig, null))
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.rapidocConfig, null))
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.swaggerUIConfig, null))
    }

    void "test render OpenApiView specification custom mapping path"() {
        given:
        String spec = "mapping.path=somewhere,redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true,openapi-explorer.enabled=true"
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
        cfg.openApiExplorerConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.rapidocConfig, null) == "/somewhere/swagger.yml"
        cfg.getSpecURL(cfg.redocConfig, null) == "/somewhere/swagger.yml"
        cfg.getSpecURL(cfg.swaggerUIConfig, null) == "/somewhere/swagger.yml"
        cfg.getSpecURL(cfg.openApiExplorerConfig, null) == "/somewhere/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("index.html"))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.redocConfig, null))
        outputDir.resolve("rapidoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.rapidocConfig, null))
        outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.swaggerUIConfig, null))
        outputDir.resolve("openapi-explorer").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.openApiExplorerConfig, null))
    }

    @RestoreSystemProperties
    void "test render OpenApiView specification with custom mapping path and server context path"() {
        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, "/context-path")
        String spec = "mapping.path=somewhere,redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true,openapi-explorer.enabled=true"
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
        cfg.openApiExplorerConfig != null
        cfg.title == "OpenAPI documentation"
        cfg.specFile == "swagger.yml"
        cfg.getSpecURL(cfg.rapidocConfig, null) == "/context-path/somewhere/swagger.yml"
        cfg.getSpecURL(cfg.redocConfig, null) == "/context-path/somewhere/swagger.yml"
        cfg.getSpecURL(cfg.swaggerUIConfig, null) == "/context-path/somewhere/swagger.yml"
        cfg.getSpecURL(cfg.openApiExplorerConfig, null) == "/context-path/somewhere/swagger.yml"
        Files.exists(outputDir.resolve("redoc").resolve("index.html"))
        Files.exists(outputDir.resolve("rapidoc").resolve("index.html"))
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))
        Files.exists(outputDir.resolve("openapi-explorer").resolve("index.html"))
        Files.notExists(outputDir.resolve("swagger-ui").resolve("oauth2-redirect.html"))
        outputDir.resolve("redoc").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.redocConfig, null))
        outputDir.resolve("openapi-explorer").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name()).contains(cfg.getSpecURL(cfg.openApiExplorerConfig, null))
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
        System.setProperty(OpenApiConfigProperty.MICRONAUT_SERVER_CONTEXT_PATH, "/local-path")
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, "/server-context-path")
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
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_SERVER_CONTEXT_PATH)
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH)
    }

    void "test generates urlResourcesPrefix only context-path"() {
        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, "/server-context-path")
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
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH)
    }

    void "test generates urlResourcesPrefix only openapi.context.path"() {
        given:
        System.setProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH, "/server-context-path")
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
        System.clearProperty(OpenApiConfigProperty.MICRONAUT_OPENAPI_CONTEXT_SERVER_PATH)
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

    void "test swaggerUi without groups"() {
        given:
        String spec = "swagger-ui.enabled=true"
        def openApiInfo = new OpenApiInfo("1", "1", "title", "swagger.yml", false, null, new OpenAPI());
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, Maps.of(Pair.NULL_STRING_PAIR, openApiInfo), new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.swaggerUIConfig?.urls.size() == 1
        cfg.render(outputDir, null)

        expect:
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))

        when:
        def indexText = outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name())

        then:
        indexText.contains(cfg.getSpecURL(cfg.swaggerUIConfig, null))
        !indexText.contains("urls:")
    }

    void "test swaggerUi with groups"() {
        given:
        String spec = "swagger-ui.enabled=true"
        def openApiInfo = new OpenApiInfo("1", "1", "title", "swagger.yml", false, null, new OpenAPI());
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, Maps.of(Pair.of("1", "1"), openApiInfo), new Properties(), null)
        Path outputDir = Paths.get("output")
        cfg.title = "OpenAPI documentation"
        cfg.specFile = "swagger.yml"
        cfg.swaggerUIConfig?.urls.size() == 1
        cfg.render(outputDir, null)

        expect:
        Files.exists(outputDir.resolve("swagger-ui").resolve("index.html"))

        when:
        def indexText = outputDir.resolve("swagger-ui").resolve("index.html").toFile().getText(StandardCharsets.UTF_8.name())

        then:
        indexText.contains(cfg.getSpecURL(cfg.swaggerUIConfig, null))
        indexText.contains("urls: [{url: contextPath + '/swagger/swagger.yml', name: '1'}],")
    }
}
