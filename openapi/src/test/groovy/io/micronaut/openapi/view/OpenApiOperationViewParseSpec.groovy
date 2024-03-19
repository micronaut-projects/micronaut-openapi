package io.micronaut.openapi.view

import spock.lang.Specification

class OpenApiOperationViewParseSpec extends Specification {

    void "test parse empty OpenApiView specification"() {
        given:
        String spec = ""
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == false
    }

    void "test parse OpenApiView specification, views enabled"() {
        given:
        String spec = "mapping.path=somewhere,redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true,openapi-explorer.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "somewhere"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.openApiExplorerConfig != null
    }

    void "test set empty string in mapping.path"() {
        given:
        String spec = "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true,openapi-explorer.enabled=true,mapping.path="
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == ""
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.openApiExplorerConfig != null
    }

    void "test parse OpenApiView specification, redoc enabled"() {
        given:
        String spec = "redoc.enabled=true,redoc.js.url=version123,redoc.spec.url=/my/spec/file.yml"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.rapidocConfig == null
        cfg.swaggerUIConfig == null
        cfg.openApiExplorerConfig == null
        cfg.redocConfig != null
        cfg.redocConfig.jsUrl == "version123"
        cfg.redocConfig.specUrl == "/my/spec/file.yml"
    }

    void "test parse OpenApiView specification, rapidoc enabled"() {
        given:
        String spec = "rapidoc.enabled=true,rapidoc.js.url=version123,rapidoc.layout=row,rapidoc.theme=light,rapidoc.spec.url=/my/spec/file.yml"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.redocConfig == null
        cfg.swaggerUIConfig == null
        cfg.openApiExplorerConfig == null
        cfg.rapidocConfig != null
        cfg.rapidocConfig.jsUrl == "version123"
        cfg.rapidocConfig.specUrl == "/my/spec/file.yml"
        cfg.rapidocConfig.options['theme'] == RapidocConfig.Theme.LIGHT
        cfg.rapidocConfig.options['layout'] == RapidocConfig.Layout.ROW
    }

    void "test parse OpenApiView specification, swagger-ui enabled"() {
        given:
        String spec = "swagger-ui.enabled=true,swagger-ui.js.url=version123,swagger-ui.spec.url=/my/spec/file.yml,swagger-ui.theme=flattop,swagger-ui.deepLinking=false,swagger-ui.persistAuthorization=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.redocConfig == null
        cfg.rapidocConfig == null
        cfg.openApiExplorerConfig == null
        cfg.swaggerUIConfig != null
        cfg.swaggerUIConfig.jsUrl == "version123"
        cfg.swaggerUIConfig.specUrl == "/my/spec/file.yml"
        cfg.swaggerUIConfig.theme == SwaggerUIConfig.Theme.FLATTOP
        cfg.swaggerUIConfig.options['deepLinking'] == false
        cfg.swaggerUIConfig.options['persistAuthorization'] == true
    }

    void "test parse OpenApiView specification, OpenAPI Explorer enabled"() {
        given:
        String spec = "openapi-explorer.enabled=true,openapi-explorer.js.url=version123,openapi-explorer.spec.url=/my/spec/file.yml"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.rapidocConfig == null
        cfg.swaggerUIConfig == null
        cfg.redocConfig == null
        cfg.openApiExplorerConfig != null
        cfg.openApiExplorerConfig.jsUrl == "version123"
        cfg.openApiExplorerConfig.specUrl == "/my/spec/file.yml"
    }
}
