package io.micronaut.openapi.view

import io.micronaut.openapi.view.OpenApiViewConfig
import spock.lang.Specification

class OpenApiOperationViewParseSpec extends Specification {

    void "test parse empty OpenApiView specification"() {
        given:
        String spec = ""
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())

        expect:
        cfg.enabled == false
    }

    void "test parse OpenApiView specification, views enabled"() {
        given:
        String spec = "mapping.path=somewhere,redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())

        expect:
        cfg.enabled == true
        cfg.mappingPath == "somewhere"
        cfg.rapidocConfig != null
        cfg.redocConfig != null
        cfg.swaggerUIConfig != null
    }

    void "test parse OpenApiView specification, redoc enabled"() {
        given:
        String spec = "redoc.enabled=true,redoc.version=version123"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.rapidocConfig == null
        cfg.swaggerUIConfig == null
        cfg.redocConfig != null
        cfg.redocConfig.version == "version123"
    }

    void "test parse OpenApiView specification, rapidoc enabled"() {
        given:
        String spec = "rapidoc.enabled=true,rapidoc.version=version123,rapidoc.layout=row,rapidoc.theme=light"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.redocConfig == null
        cfg.swaggerUIConfig == null
        cfg.rapidocConfig != null
        cfg.rapidocConfig.version == "version123"
        cfg.rapidocConfig.options['theme'] == RapidocConfig.Theme.LIGHT
        cfg.rapidocConfig.options['layout'] == RapidocConfig.Layout.ROW
    }

    void "test parse OpenApiView specification, swagger-ui enabled"() {
        given:
        String spec = "swagger-ui.enabled=true,swagger-ui.version=version123,swagger-ui.theme=flattop,swagger-ui.deepLinking=false"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec, new Properties())

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.redocConfig == null
        cfg.rapidocConfig == null
        cfg.swaggerUIConfig != null
        cfg.swaggerUIConfig.version == "version123"
        cfg.swaggerUIConfig.theme == SwaggerUIConfig.Theme.FLATTOP
        cfg.swaggerUIConfig.options['deepLinking'] == false
    }
}
