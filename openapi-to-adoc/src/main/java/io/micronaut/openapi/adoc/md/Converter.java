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
package io.micronaut.openapi.adoc.md;

import java.util.Collection;
import java.util.Map;

import io.micronaut.openapi.adoc.utils.CollectionUtils;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.RootNode;

/**
 * Convert-methods from MD format to AsciiDoc.
 *
 * @since 5.2.0
 */
public final class Converter {

    private Converter() {
    }

    /**
     * Convert Markdown text to Asciidoc.
     *
     * @param markdown Markdown text
     *
     * @return Asciidoc text
     */
    public static String convertMdToAdoc(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }
        var processor = new PegDownProcessor(Extensions.ALL);
        // insert blank line before fenced code block if necessary
        if (markdown.contains("```")) {
            markdown = markdown.replaceAll("(?m)(?<!\n\n)(\\s*)```(\\w*\n)((?:\\1[^\n]*\n)+)\\1```", "\n$1```$2$3$1```");
        }
        RootNode rootNode = processor.parseMarkdown(markdown.toCharArray());
        return new ToAsciiDocSerializer(rootNode, markdown)
            .toAsciiDoc();
    }

    /**
     * Convert all OpenAPI description fields from Markdown format to Asciidoc format.
     *
     * @param openApi OpenAPI object
     */
    public static void convertMdToAdoc(OpenAPI openApi) {
        var info = openApi.getInfo();
        if (info != null) {
            info.setDescription(convertMdToAdoc(info.getDescription()));
            info.setTermsOfService(convertMdToAdoc(info.getTermsOfService()));
        }
        processExternalDocs(openApi.getExternalDocs());

        var servers = openApi.getServers();
        if (CollectionUtils.isNotEmpty(servers)) {
            for (var server : servers) {
                server.setDescription(convertMdToAdoc(server.getDescription()));
                if (CollectionUtils.isNotEmpty(server.getVariables())) {
                    for (var serverVar : server.getVariables().values()) {
                        serverVar.setDescription(convertMdToAdoc(serverVar.getDescription()));
                    }
                }
            }
        }

        var tags = openApi.getTags();
        if (CollectionUtils.isNotEmpty(tags)) {
            for (var tag : tags) {
                tag.setDescription(convertMdToAdoc(tag.getDescription()));
                processExternalDocs(tag.getExternalDocs());
            }
        }

        var paths = openApi.getPaths();
        if (CollectionUtils.isNotEmpty(paths)) {
            for (var path : paths.values()) {
                path.setSummary(convertMdToAdoc(path.getSummary()));
                path.setDescription(convertMdToAdoc(path.getDescription()));
                for (var operation : path.readOperations()) {
                    operation.setSummary(convertMdToAdoc(operation.getSummary()));
                    operation.setDescription(convertMdToAdoc(operation.getDescription()));
                    processExternalDocs(operation.getExternalDocs());
                    var requestBody = operation.getRequestBody();
                    if (requestBody != null) {
                        requestBody.setDescription(convertMdToAdoc(requestBody.getDescription()));
                        processContent(requestBody.getContent());
                    }
                    if (CollectionUtils.isNotEmpty(operation.getParameters())) {
                        for (var parameter : operation.getParameters()) {
                            processSchema(parameter.getSchema());
                            processExamples(parameter.getExamples());
                            processContent(parameter.getContent());
                            parameter.setDescription(convertMdToAdoc(parameter.getDescription()));
                        }
                    }
                    processResponses(operation.getResponses());
                }
            }
        }

        if (openApi.getComponents() != null) {
            processSchemas(openApi.getComponents().getSchemas());
            processResponses(openApi.getComponents().getResponses());
            if (CollectionUtils.isNotEmpty(openApi.getComponents().getParameters())) {
                processParameters(openApi.getComponents().getParameters().values());
            }
            processExamples(openApi.getComponents().getExamples());
            processRequestBodies(openApi.getComponents().getRequestBodies().values());
            processHeaders(openApi.getComponents().getHeaders());
            processSecuritySchemas(openApi.getComponents().getSecuritySchemes().values());
            processLinks(openApi.getComponents().getLinks());
        }
    }

    private static void processExternalDocs(ExternalDocumentation externalDocs) {
        if (externalDocs == null) {
            return;
        }
        externalDocs.setDescription(convertMdToAdoc(externalDocs.getDescription()));
    }

    private static void processSchemas(Map<String, Schema> schemas) {
        if (CollectionUtils.isEmpty(schemas)) {
            return;
        }
        for (var schema : schemas.values()) {
            processSchema(schema);
        }
    }

    private static void processSchema(Schema schema) {
        if (schema == null) {
            return;
        }
        processExternalDocs(schema.getExternalDocs());
        schema.setDescription(convertMdToAdoc(schema.getDescription()));
        processSchemas(schema.getProperties());
        processSchema(schema.getItems());
    }

    private static void processHeaders(Map<String, Header> headers) {
        if (CollectionUtils.isEmpty(headers)) {
            return;
        }
        for (var header : headers.values()) {
            header.setDescription(convertMdToAdoc(header.getDescription()));
            processExamples(header.getExamples());
            processSchema(header.getSchema());
            processContent(header.getContent());
        }
    }

    private static void processExamples(Map<String, Example> examples) {
        if (CollectionUtils.isEmpty(examples)) {
            return;
        }
        for (var example : examples.values()) {
            example.setSummary(convertMdToAdoc(example.getSummary()));
            example.setDescription(convertMdToAdoc(example.getDescription()));
        }
    }

    private static void processContent(Content content) {
        if (content == null) {
            return;
        }

        for (var mediaType : content.values()) {
            processSchema(mediaType.getSchema());
            processExamples(mediaType.getExamples());
            processSchema(mediaType.getSchema());
            if (CollectionUtils.isNotEmpty(mediaType.getEncoding())) {
                for (var encoding : mediaType.getEncoding().values()) {
                    processHeaders(encoding.getHeaders());
                }
            }
        }
    }

    private static void processResponses(Map<String, ApiResponse> responses) {
        if (CollectionUtils.isEmpty(responses)) {
            return;
        }

        for (var response : responses.values()) {
            processHeaders(response.getHeaders());
            processContent(response.getContent());
            response.setDescription(convertMdToAdoc(response.getDescription()));
            processLinks(response.getLinks());
            if (CollectionUtils.isNotEmpty(response.getLinks())) {
                for (var link : response.getLinks().values()) {
                    link.setDescription(convertMdToAdoc(link.getDescription()));
                }
            }
        }
    }

    private static void processLinks(Map<String, Link> links) {
        if (CollectionUtils.isEmpty(links)) {
            return;
        }

        for (var link : links.values()) {
            link.setDescription(convertMdToAdoc(link.getDescription()));
        }
    }

    private static void processParameters(Collection<Parameter> parameters) {
        if (CollectionUtils.isEmpty(parameters)) {
            return;
        }

        for (var parameter : parameters) {
            processSchema(parameter.getSchema());
            processExamples(parameter.getExamples());
            processContent(parameter.getContent());
            parameter.setDescription(convertMdToAdoc(parameter.getDescription()));
        }
    }

    private static void processRequestBodies(Collection<RequestBody> requestBodies) {
        if (CollectionUtils.isEmpty(requestBodies)) {
            return;
        }

        for (var requestBody : requestBodies) {
            requestBody.setDescription(convertMdToAdoc(requestBody.getDescription()));
            processContent(requestBody.getContent());
        }
    }

    private static void processSecuritySchemas(Collection<SecurityScheme> securitySchemes) {
        if (CollectionUtils.isEmpty(securitySchemes)) {
            return;
        }

        for (var securityScheme : securitySchemes) {
            securityScheme.setDescription(convertMdToAdoc(securityScheme.getDescription()));
        }
    }
}
