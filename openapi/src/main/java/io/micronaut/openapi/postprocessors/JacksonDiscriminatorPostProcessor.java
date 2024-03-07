/*
 * Copyright 2017-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.postprocessors;

import java.util.ArrayList;
import java.util.List;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import static io.micronaut.openapi.visitor.SchemaUtils.setSpecVersion;
import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;

/**
 * Utility class to add missing "discriminator" property when using Jackson {@link com.fasterxml.jackson.annotation.JsonTypeInfo}
 * and {@link com.fasterxml.jackson.annotation.JsonSubTypes}.
 *
 * @author Iván López
 * @since 3.0.0
 */
public class JacksonDiscriminatorPostProcessor {

    /**
     * Add the missing discriminator property to the schemas related to another schema referencing them.
     *
     * @param openAPI The OpenAPI object
     */
    public void addMissingDiscriminatorType(OpenAPI openAPI) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return;
        }

        for (Schema<?> schema : openAPI.getComponents().getSchemas().values()) {
            if (schema.getDiscriminator() != null && schema.getDiscriminator().getMapping() != null) {
                String discriminatorProperty = schema.getDiscriminator().getPropertyName();
                var schemasToUpdate = new ArrayList<>(schema.getDiscriminator().getMapping().values());
                addDiscriminatorProperty(openAPI, schemasToUpdate, discriminatorProperty);
            }
        }
    }

    private void addDiscriminatorProperty(OpenAPI openAPI, List<String> schemasToUpdate, @NonNull String discriminatorProperty) {
        for (String s : schemasToUpdate) {
            Schema<?> schema = openAPI.getComponents().getSchemas().get(extractComponentSchemaName(s));
            if (schema.getProperties() != null && !schema.getProperties().containsKey(discriminatorProperty)) {
                schema.addProperty(discriminatorProperty, setSpecVersion(new StringSchema()));
            }
        }
    }

    private String extractComponentSchemaName(@NonNull String mapping) {
        return mapping.replace(COMPONENTS_SCHEMAS_REF, StringUtils.EMPTY_STRING);
    }
}
