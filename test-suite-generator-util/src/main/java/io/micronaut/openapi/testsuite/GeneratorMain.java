/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.testsuite;

import io.micronaut.openapi.generator.MicronautCodeGeneratorEntryPoint;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import static io.micronaut.openapi.generator.MicronautCodeGeneratorEntryPoint.OutputKind.*;

/**
 * An entry point to be used in tests, to simulate
 * what the Micronaut OpenAPI Gradle plugin would do
 */
public class GeneratorMain {
    public static void main(String[] args) throws URISyntaxException {
        boolean server = "server".equals(args[0]);
        var builder = MicronautCodeGeneratorEntryPoint.builder()
            .withDefinitionFile(new URI(args[1]))
            .withOutputDirectory(new File(args[2]))
            .withOutputs(
                MODELS,
                APIS,
                SUPPORTING_FILES
            )
            .withOptions(options -> {
                options.withInvokerPackage("io.micronaut.openapi.test");
                options.withApiPackage("io.micronaut.openapi.test.api");
                options.withModelPackage("io.micronaut.openapi.test.model");
                options.withBeanValidation(true);
                options.withOptional(true);
                options.withReactive(true);
                options.withTestFramework(MicronautCodeGeneratorEntryPoint.TestFramework.SPOCK);
            });
        if (server) {
            builder.forServer(serverOptions -> {
                serverOptions.withControllerPackage("io.micronaut.openapi.test.controller");
                // commented out because currently this would prevent the test project from compiling
                // because we generate both abstract classes _and_ dummy implementations
                 serverOptions.withGenerateAbstractClasses(false);
                 serverOptions.withAuthentication(false);
            });
        } else {
            builder.forClient(client -> {
            });
        }
        builder.build().generate();
    }
}
