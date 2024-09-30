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
package io.micronaut.openapi.visitor;

import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.adoc.OpenApiToAdocConverter;
import io.micronaut.openapi.visitor.group.OpenApiInfo;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import static io.micronaut.openapi.visitor.ContextUtils.addGeneratedResource;
import static io.micronaut.openapi.visitor.ContextUtils.info;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.FileUtils.EXT_ADOC;
import static io.micronaut.openapi.visitor.FileUtils.EXT_JSON;
import static io.micronaut.openapi.visitor.FileUtils.EXT_YAML;
import static io.micronaut.openapi.visitor.FileUtils.EXT_YML;
import static io.micronaut.openapi.visitor.FileUtils.createDirectories;
import static io.micronaut.openapi.visitor.FileUtils.getDefaultFilePath;
import static io.micronaut.openapi.visitor.FileUtils.resolve;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME;

/**
 * Method to convert final openapi file to adoc format.
 *
 * @since 5.2.0
 */
public final class AdocModule {

    private AdocModule() {
    }

    /**
     * Convert and save to file openAPI object in adoc format.
     *
     * @param openApiInfo openApiInfo object
     * @param props openapi-adoc properties
     * @param context visitor context
     */
    public static void convert(OpenApiInfo openApiInfo, Map<String, String> props, VisitorContext context) {

        try {
            var writer = new StringWriter();
            OpenApiToAdocConverter.convert(openApiInfo.getOpenApi(), props, writer);

            var adoc = writer.toString();

            var outputPath = getOutputPath(openApiInfo, props, context);
            info("Writing AsciiDoc OpenAPI file to destination: " + outputPath, context);
            var classesOutputPath = ContextUtils.getClassesOutputPath(context);
            if (classesOutputPath != null) {
                // add relative paths for the specPath, and its parent META-INF/swagger
                // so that micronaut-graal visitor knows about them
                addGeneratedResource(classesOutputPath.relativize(outputPath).toString(), context);
            }

            if (Files.exists(outputPath)) {
                Files.writeString(outputPath, adoc, StandardOpenOption.APPEND);
            } else {
                Files.writeString(outputPath, adoc);
            }
        } catch (Exception e) {
            warn("Can't convert to ADoc format\n" + Utils.printStackTrace(e), context);
        }
    }

    private static Path getOutputPath(OpenApiInfo openApiInfo, Map<String, String> props, VisitorContext context) {

        var fileName = props.get(MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME);
        if (StringUtils.isEmpty(fileName)) {

            var openApiFilename = openApiInfo.getFilename();

            if (openApiFilename.endsWith(EXT_JSON)
                || openApiFilename.endsWith(EXT_YML)
                || openApiFilename.endsWith(EXT_YAML)) {
                fileName = openApiFilename.substring(0, openApiFilename.lastIndexOf('.'));
            }
            fileName += EXT_ADOC;
        }

        Path outputPath;
        String outputDir = props.get(MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH);
        if (StringUtils.isNotEmpty(outputDir)) {
            outputPath = resolve(context, Paths.get(outputDir));
        } else {
            var defaultFilePath = getDefaultFilePath(fileName, context);
            if (defaultFilePath == null) {
                warn("Can't read defaultFilePath property", context);
                throw new RuntimeException("Can't read defaultFilePath property");
            }
            outputPath = defaultFilePath.getParent();
        }
        outputPath = outputPath.resolve(fileName);
        createDirectories(outputPath, context);

        return outputPath;
    }
}
