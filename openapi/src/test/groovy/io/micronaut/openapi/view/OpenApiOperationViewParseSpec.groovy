package io.micronaut.openapi.view

import io.micronaut.openapi.visitor.Pair
import io.micronaut.openapi.visitor.group.OpenApiInfo
import io.swagger.v3.oas.models.OpenAPI
import spock.lang.Specification

class OpenApiOperationViewParseSpec extends Specification {

    void "test set mapping.path with groups"() {
        given:
        var groupOpenApiInfo = new OpenApiInfo(new OpenAPI());
        groupOpenApiInfo.groupName = "Group1"
        groupOpenApiInfo.filename = "swagger-group1.yml"
        String spec = "swagger-ui.enabled=true,mapping.path=/some/papping/path"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, Map.of(
                Pair.of("group1", null), groupOpenApiInfo
        ), new Properties(), null)

        expect:
        cfg.mappingPath == "/some/papping/path"
        cfg.swaggerUIConfig.urls
        cfg.swaggerUIConfig.urls.size() == 1
        cfg.swaggerUIConfig.urls[0].url() == "/some/papping/path/swagger-group1.yml"
        cfg.swaggerUIConfig.urls[0].name() == "Group1"
    }

    void "test parse empty OpenApiView specification"() {
        given:
        String spec = ""
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == false
    }

    void "test parse OpenApiView specification, views enabled"() {
        given:
        String spec = "mapping.path=somewhere,redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true,openapi-explorer.enabled=true,scalar.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "somewhere"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.openApiExplorerConfig != null
        cfg.scalarConfig != null
    }

    void "test set empty string in mapping.path"() {
        given:
        String spec = "redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true,openapi-explorer.enabled=true,scalar.enabled=true,mapping.path="
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == ""
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
        cfg.openApiExplorerConfig != null
        cfg.scalarConfig != null
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
        cfg.scalarConfig == null
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
        cfg.scalarConfig == null
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
        cfg.scalarConfig == null
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
        cfg.scalarConfig == null
        cfg.openApiExplorerConfig != null
        cfg.openApiExplorerConfig.jsUrl == "version123"
        cfg.openApiExplorerConfig.specUrl == "/my/spec/file.yml"
    }

    void "test parse OpenApiView specification, Scalar enabled"() {
        given:
        String spec = "scalar.enabled=true,scalar.js.url=version123,scalar.spec.url=/my/spec/file.yml"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, null, new Properties(), null)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.rapidocConfig == null
        cfg.swaggerUIConfig == null
        cfg.redocConfig == null
        cfg.openApiExplorerConfig == null
        cfg.scalarConfig != null
        cfg.scalarConfig.jsUrl == "version123"
        cfg.scalarConfig.specUrl == "/my/spec/file.yml"
    }
}
