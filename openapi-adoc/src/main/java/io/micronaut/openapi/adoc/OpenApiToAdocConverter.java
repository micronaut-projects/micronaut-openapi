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
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.adoc.md.MdToAdocConverter;
import io.micronaut.openapi.adoc.utils.SwaggerUtils;
import io.swagger.v3.oas.models.OpenAPI;

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
public final class OpenApiToAdocConverter {

    private static final String TEMPLATE_PREFIX = "template_";
    private static final String TEMPLATES_DIR = "/template";

    private OpenApiToAdocConverter() {
    }

    /**
     * Conversion from openAPI format to asciidoc format.
     *
     * @throws TemplateException som problems with freemarker templates
     * @throws IOException some problems with files
     */
    public static void convert() throws TemplateException, IOException {
        var openApiFile = System.getProperty(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_OPENAPI_PATH);
        if (openApiFile == null || openApiFile.isBlank()) {
            throw new IllegalArgumentException("OpenAPI file path not set");
        }
        var openApi = SwaggerUtils.readOpenApiFromLocation(openApiFile);

        var outputPath = Paths.get(System.getProperty(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_DIR_PATH, "build/generated"))
            .resolve(System.getProperty(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_OUTPUT_FILENAME, "openApiDoc.adoc"));

        if (outputPath.getParent() != null) {
            try {
                Files.createDirectories(outputPath.getParent());
            } catch (IOException e) {
                throw new RuntimeException("Failed create directory", e);
            }
        }

        var fileExists = Files.exists(outputPath);
        try (var writer = fileExists ? Files.newBufferedWriter(outputPath, UTF_8, StandardOpenOption.APPEND) : Files.newBufferedWriter(outputPath, UTF_8)) {
            convert(openApi, System.getProperties(), writer);
        }
    }

    /**
     * Conversion from openAPI format to asciidoc format.
     *
     * @param openApi openAPI object
     * @param props converter config properties
     * @param writer writer for rendered template
     *
     * @throws TemplateException som problems with freemarker templates
     * @throws IOException some problems with files
     */
    public static void convert(OpenAPI openApi, Map props, Writer writer) throws TemplateException, IOException {

        MdToAdocConverter.convert(openApi);

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
            if (key.startsWith(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATE_PREFIX)) {
                model.put(key.replace(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATE_PREFIX, TEMPLATE_PREFIX), entry.getValue());
            }
        }

        var templateFilename = System.getProperty(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATE_FILENAME, "openApiDoc.ftl");
        var customTemplatesDirsStr = System.getProperty(OpenApiToAdocConfigProperty.MICRONAUT_OPENAPI_ADOC_TEMPLATES_DIR_PATH);
        String[] customTemplatesDirs = null;
        if (customTemplatesDirsStr != null && !customTemplatesDirsStr.isBlank()) {
            customTemplatesDirs = customTemplatesDirsStr.split(",");
        }

        var cfg = getFreemarkerConfig(customTemplatesDirs);
        var template = cfg.getTemplate(templateFilename);

        template.process(model, writer);
    }

    private static Configuration getFreemarkerConfig(String[] customTemplatesDirs) throws IOException, TemplateModelException {
        TemplateLoader templateLoader = new ClassTemplateLoader(OpenApiToAdocConverter.class, TEMPLATES_DIR);
        if (customTemplatesDirs != null && customTemplatesDirs.length > 0) {
            var templateLoaders = new ArrayList<TemplateLoader>();
            for (var templateDir : customTemplatesDirs) {
                templateDir = templateDir.strip()
                    .replace("\\", "/");
                if (templateDir.startsWith(CLASSPATH_SCHEME)) {
                    templateLoaders.add(new ClassTemplateLoader(OpenApiToAdocConverter.class, templateDir.substring(CLASSPATH_SCHEME.length())));
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

    private static String template(String templateName) {
        return TEMPLATE_PREFIX + templateName;
    }
}
