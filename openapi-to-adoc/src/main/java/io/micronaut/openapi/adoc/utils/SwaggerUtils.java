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
package io.micronaut.openapi.adoc.utils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.micronaut.core.annotation.Internal;
import io.micronaut.openapi.OpenApiUtils;
import io.swagger.v3.oas.models.OpenAPI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static io.micronaut.openapi.adoc.utils.FileUtils.FILE_SCHEME;
import static io.micronaut.openapi.adoc.utils.FileUtils.loadFileFromClasspath;

/**
 * File utilities methods.
 *
 * @since 5.2.0
 */
@Internal
public final class SwaggerUtils {

    private SwaggerUtils() {
    }

    public static OpenAPI readOpenApi(String swaggerFileContent, boolean isJson) {

        ObjectMapper mapper;
        if (isJson) {
            mapper = OpenApiUtils.getJsonMapper();
        } else {
            mapper = OpenApiUtils.getYamlMapper();
        }

        try {
            return mapper.readValue(swaggerFileContent, OpenAPI.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't parse swagger file", e);
        }
    }

    public static OpenAPI readOpenApiFromLocation(String location) {

        var isJson = location.endsWith(".json");
        var adjustedLocation = location.replaceAll("\\\\", "/").toLowerCase();
        try {
            if (adjustedLocation.startsWith("jar:")) {
                try (var in = new URI(adjustedLocation).toURL().openStream()) {
                    return readOpenApi(new String(in.readAllBytes(), StandardCharsets.UTF_8), isJson);
                }
            } else {
                var path = adjustedLocation.startsWith(FILE_SCHEME) ?
                    Paths.get(URI.create(adjustedLocation)) : Paths.get(adjustedLocation);
                if (Files.exists(path)) {
                    return readOpenApi(Files.readString(path), isJson);
                } else {
                    return readOpenApi(loadFileFromClasspath(adjustedLocation), isJson);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to read location `%s`".formatted(adjustedLocation), e);
        }
    }
}
