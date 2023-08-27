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
package io.micronaut.openapi.adoc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;

import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.adoc.utils.SwaggerUtils;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

import static io.micronaut.openapi.adoc.TemplatePaths.CONTENT;
import static io.micronaut.openapi.adoc.TemplatePaths.DEFINITIONS;
import static io.micronaut.openapi.adoc.TemplatePaths.EXAMPLES;
import static io.micronaut.openapi.adoc.TemplatePaths.EXTERNAL_DOCS;
import static io.micronaut.openapi.adoc.TemplatePaths.HEADERS;
import static io.micronaut.openapi.adoc.TemplatePaths.LINKS;
import static io.micronaut.openapi.adoc.TemplatePaths.OVERVIEW;
import static io.micronaut.openapi.adoc.TemplatePaths.PARAMETERS;
import static io.micronaut.openapi.adoc.TemplatePaths.PATHS;
import static io.micronaut.openapi.adoc.TemplatePaths.PROPERTIES;
import static io.micronaut.openapi.adoc.TemplatePaths.PROPERTY_DESCRIPTION;
import static io.micronaut.openapi.adoc.TemplatePaths.REQUEST_BODY;
import static io.micronaut.openapi.adoc.TemplatePaths.RESPONSES;
import static io.micronaut.openapi.adoc.TemplatePaths.SCHEMA_TYPE;
import static io.micronaut.openapi.adoc.TemplatePaths.SECURITY_REQUIREMENTS;
import static io.micronaut.openapi.adoc.TemplatePaths.SERVERS;
import static io.micronaut.openapi.adoc.utils.FileUtils.CLASSPATH_SCHEME;
import static io.micronaut.openapi.adoc.utils.FileUtils.FILE_SCHEME;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * OpenAPI to Asciidoc converter.
 *
 * @since 4.2.0
 */
public class OpenapiToAdocConverter {

    private static final String TEMPLATE_PREFIX = "template_";
    private static final String TEMPLATES_DIR = "/template";

    /**
     * Convertion from openAPI format to asciidoc format.
     *
     * @throws TemplateException som problms with freemarker templates
     * @throws IOException some problems with files
     */
    public void convert() throws TemplateException, IOException {

        var openApiFile = System.getProperty(ConfigProperty.OPENAPIDOC_OPENAPI_PATH);
        if (openApiFile == null || openApiFile.isBlank()) {
            throw new IllegalArgumentException("OpenAPI file path not set");
        }
        var openApi = SwaggerUtils.readOpenApiFromLocation(openApiFile);

        var model = new HashMap<String, Object>();
        model.put("info", openApi.getInfo());
        model.put("externalDocs", openApi.getExternalDocs());
        model.put("servers", openApi.getServers());
        model.put("tags", openApi.getTags());
        model.put("openApi", openApi.getOpenapi());
        model.put("globalSecurityRequirements", openApi.getSecurity());
        model.put("paths", openApi.getPaths());
        model.put("components", openApi.getComponents());

        model.put(template(DEFINITIONS), "definitions.ftl");
        model.put(template(OVERVIEW), "overview.ftl");
        model.put(template(PATHS), "paths.ftl");
        model.put(template(CONTENT), "content.ftl");
        model.put(template(EXAMPLES), "examples.ftl");
        model.put(template(EXTERNAL_DOCS), "externalDocs.ftl");
        model.put(template(HEADERS), "headers.ftl");
        model.put(template(LINKS), "links.ftl");
        model.put(template(PARAMETERS), "parameters.ftl");
        model.put(template(PROPERTIES), "properties.ftl");
        model.put(template(PROPERTY_DESCRIPTION), "propertyDescription.ftl");
        model.put(template(REQUEST_BODY), "requestBody.ftl");
        model.put(template(RESPONSES), "responses.ftl");
        model.put(template(SCHEMA_TYPE), "schemaType.ftl");
        model.put(template(SECURITY_REQUIREMENTS), "securityRequirements.ftl");
        model.put(template(SERVERS), "servers.ftl");

        for (var entry : System.getProperties().entrySet()) {
            var key = entry.getKey().toString();
            if (key.startsWith(ConfigProperty.OPENAPIDOC_TEMPLATE_PREFIX)) {
                model.put(key.replace(ConfigProperty.OPENAPIDOC_TEMPLATE_PREFIX, TEMPLATE_PREFIX), entry.getValue());
            }
        }

        var templateFilename = System.getProperty(ConfigProperty.OPENAPIDOC_TEMPLATE_FILENAME, "openApiDoc.ftl");
        var outputPath = Paths.get(System.getProperty(ConfigProperty.OPENAPIDOC_OUTPUT_DIR_PATH, "build/generated"))
            .resolve(System.getProperty(ConfigProperty.OPENAPIDOC_OUTPUT_FILENAME, "openApiDoc.adoc"));
        var customTemplatesDirsStr = System.getProperty(ConfigProperty.OPENAPIDOC_TEMPLATES_DIR_PATH);
        String[] customTemplatesDirs = null;
        if (customTemplatesDirsStr != null && !customTemplatesDirsStr.isBlank()) {
            customTemplatesDirs = customTemplatesDirsStr.split(",");
        }

        var cfg = getFreemarkerConfig(customTemplatesDirs);
        var template = cfg.getTemplate(templateFilename);

        if (outputPath.getParent() != null) {
            try {
                Files.createDirectories(outputPath.getParent());
            } catch (IOException e) {
                throw new RuntimeException("Failed create directory", e);
            }
        }

        var fileExists = Files.exists(outputPath);
        try (var writer = fileExists ? Files.newBufferedWriter(outputPath, UTF_8, StandardOpenOption.APPEND) : Files.newBufferedWriter(outputPath, UTF_8)) {
            template.process(model, writer);
        }
    }

    private Configuration getFreemarkerConfig(String[] customTemplatesDirs) throws IOException, TemplateModelException {
        TemplateLoader templateLoader = new ClassTemplateLoader(getClass(), TEMPLATES_DIR);
        if (customTemplatesDirs != null && customTemplatesDirs.length > 0) {
            var templateLoaders = new ArrayList<TemplateLoader>();
            for (var templateDir : customTemplatesDirs) {
                templateDir = templateDir.strip();
                if (templateDir.startsWith(CLASSPATH_SCHEME)) {
                    templateLoaders.add(new ClassTemplateLoader(getClass(), templateDir.substring(CLASSPATH_SCHEME.length())));
                } else {
                    if (templateDir.startsWith(FILE_SCHEME)) {
                        templateDir = templateDir.substring(FILE_SCHEME.length());
                        if (templateDir.startsWith("//")) {
                            templateDir = templateDir.substring(2);
                        }
                    }
                    templateLoaders.add(new FileTemplateLoader(new File(templateDir)));
                }
            }
            templateLoaders.add(templateLoader);
            templateLoader = new MultiTemplateLoader(templateLoaders.toArray(new TemplateLoader[0]));
        }

        var cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setTemplateLoader(templateLoader);
        cfg.setDefaultEncoding(UTF_8.displayName());
        cfg.setSharedVariable("JSON", OpenApiUtils.getJsonMapper());
        return cfg;
    }

    private String template(String templateName) {
        return TEMPLATE_PREFIX + templateName;
    }
}
