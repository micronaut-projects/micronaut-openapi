/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.openapi.visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.micronaut.core.annotation.Internal;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.EmailSchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.PasswordSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;

import static io.micronaut.openapi.visitor.Utils.resolveComponents;
import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;

/**
 * Some schema util methods.
 *
 * @since 4.5.0
 */
@Internal
public final class SchemaUtils {

    public static final String COMPONENTS_CALLBACKS_PREFIX = "#/components/callbacks/";
    public static final String COMPONENTS_SCHEMAS_PREFIX = "#/components/schemas/";

    public static final Schema<?> EMPTY_SCHEMA = new Schema<>();
    public static final Schema<?> EMPTY_ARRAY_SCHEMA = new ArraySchema();
    public static final Schema<?> EMPTY_BINARY_SCHEMA = new BinarySchema();
    public static final Schema<?> EMPTY_BOOLEAN_SCHEMA = new BooleanSchema();
    public static final Schema<?> EMPTY_BYTE_ARRAY_SCHEMA = new ByteArraySchema();
    public static final Schema<?> EMPTY_COMPOSED_SCHEMA = new ComposedSchema();
    public static final Schema<?> EMPTY_DATE_SCHEMA = new DateSchema();
    public static final Schema<?> EMPTY_DATE_TIME_SCHEMA = new DateTimeSchema();
    public static final Schema<?> EMPTY_EMAIL_SCHEMA = new EmailSchema();
    public static final Schema<?> EMPTY_FILE_SCHEMA = new FileSchema();
    public static final Schema<?> EMPTY_INTEGER_SCHEMA = new IntegerSchema();
    public static final Schema<?> EMPTY_JSON_SCHEMA = new JsonSchema();
    public static final Schema<?> EMPTY_MAP_SCHEMA = new MapSchema();
    public static final Schema<?> EMPTY_NUMBER_SCHEMA = new NumberSchema();
    public static final Schema<?> EMPTY_OBJECT_SCHEMA = new ObjectSchema();
    public static final Schema<?> EMPTY_PASSWORD_SCHEMA = new PasswordSchema();
    public static final Schema<?> EMPTY_STRING_SCHEMA = new StringSchema();
    public static final Schema<?> EMPTY_UUID_SCHEMA = new UUIDSchema();
    public static final Schema<?> EMPTY_SIMPLE_SCHEMA = new SimpleSchema();

    public static final String TYPE_OBJECT = "object";

    private static final List<Schema<?>> ALL_EMPTY_SCHEMAS;

    static {
        List<Schema<?>> schemas = new ArrayList<>();
        schemas.add(EMPTY_SCHEMA);
        schemas.add(EMPTY_ARRAY_SCHEMA);
        schemas.add(EMPTY_BINARY_SCHEMA);
        schemas.add(EMPTY_BOOLEAN_SCHEMA);
        schemas.add(EMPTY_BYTE_ARRAY_SCHEMA);
        schemas.add(EMPTY_COMPOSED_SCHEMA);
        schemas.add(EMPTY_DATE_SCHEMA);
        schemas.add(EMPTY_DATE_TIME_SCHEMA);
        schemas.add(EMPTY_EMAIL_SCHEMA);
        schemas.add(EMPTY_FILE_SCHEMA);
        schemas.add(EMPTY_INTEGER_SCHEMA);
        schemas.add(EMPTY_JSON_SCHEMA);
        schemas.add(EMPTY_MAP_SCHEMA);
        schemas.add(EMPTY_NUMBER_SCHEMA);
        schemas.add(EMPTY_OBJECT_SCHEMA);
        schemas.add(EMPTY_PASSWORD_SCHEMA);
        schemas.add(EMPTY_STRING_SCHEMA);
        schemas.add(EMPTY_UUID_SCHEMA);
        schemas.add(EMPTY_SIMPLE_SCHEMA);

        ALL_EMPTY_SCHEMAS = Collections.unmodifiableList(schemas);
    }

    private SchemaUtils() {
    }

    public static boolean isEmptySchema(Schema<?> schema) {
        return ALL_EMPTY_SCHEMAS.contains(schema);
    }

    public static Map<String, Schema> resolveSchemas(OpenAPI openAPI) {
        Components components = resolveComponents(openAPI);
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) {
            schemas = new LinkedHashMap<>();
            components.setSchemas(schemas);
        }
        return schemas;
    }

    public static ArraySchema arraySchema(Schema schema) {
        if (schema == null) {
            return null;
        }
        ArraySchema arraySchema = new ArraySchema();
        arraySchema.setItems(schema);
        return arraySchema;
    }

    public static String schemaRef(String schemaName) {
        return COMPONENTS_SCHEMAS_REF + schemaName;
    }
}
