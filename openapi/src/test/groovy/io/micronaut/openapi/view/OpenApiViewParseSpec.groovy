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

import io.micronaut.openapi.view.OpenApiViewConfig
import io.micronaut.openapi.view.OpenApiViewConfig.RapidocConfig
import io.micronaut.openapi.view.OpenApiViewConfig.SwaggerUIConfig
import spock.lang.Specification

class OpenApiOperationViewParseSpec extends Specification {

    void "test parse empty OpenApiView specification"() {
        given:
        String spec = ""
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec)

        expect:
        cfg.enabled == false
    }

    void "test parse OpenApiView specification, views enabled"() {
        given:
        String spec = "mapping.path=somewhere,redoc.enabled=true,rapidoc.enabled=true,swagger-ui.enabled=true"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "somewhere"
        cfg.rapidoc != null
        cfg.redoc != null
        cfg.swaggerUi != null
    }
    
    void "test parse OpenApiView specification, redoc enabled"() {
        given:
        String spec = "redoc.enabled=true,redoc.version=version123"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.rapidoc == null
        cfg.swaggerUi == null
        cfg.redoc != null
        cfg.redoc.version == "version123"
    }
    
    void "test parse OpenApiView specification, rapidoc enabled"() {
        given:
        String spec = "rapidoc.enabled=true,rapidoc.version=version123,rapidoc.layout=row,rapidoc.theme=light"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.redoc == null
        cfg.swaggerUi == null
        cfg.rapidoc != null
        cfg.rapidoc.version == "version123"
        cfg.rapidoc.theme == RapidocConfig.Theme.LIGHT
        cfg.rapidoc.layout == RapidocConfig.Layout.ROW
    }
    
    void "test parse OpenApiView specification, swagger-ui enabled"() {
        given:
        String spec = "swagger-ui.enabled=true,swagger-ui.version=version123,swagger-ui.theme=flattop,swagger-ui.deep-linking=false"
        OpenApiViewConfig cfg = OpenApiViewConfig.fromSpecification(spec)

        expect:
        cfg.enabled == true
        cfg.mappingPath == "swagger"
        cfg.redoc == null
        cfg.rapidoc == null
        cfg.swaggerUi != null
        cfg.swaggerUi.version == "version123"
        cfg.swaggerUi.theme == SwaggerUIConfig.Theme.FLATTOP
        cfg.swaggerUi.deepLinking == false
    }
}
