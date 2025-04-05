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
package io.micronaut.build.internal.openapi;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

@CacheableTask
public abstract class RemoteDownloadTask extends DefaultTask {
    @Input
    abstract Property<String> getContentPath();

    @Input
    abstract Property<String> getBaseUrl();

    @Input
    abstract Property<String> getOutputPath();

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    void download() {
        var client = HttpClient.newHttpClient();
        var baseUrl = getBaseUrl().get();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        try {
            var contentPath = getContentPath().get();
            var uri = new URI(baseUrl + contentPath);
            getLogger().lifecycle("Downloading {}", uri);
            var request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build();
            var outputFilePath = getOutputDirectory().get().getAsFile().toPath().resolve(getOutputPath().get());
            var parentDir = outputFilePath.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            client.send(request, HttpResponse.BodyHandlers.ofFile(outputFilePath));
        } catch (InterruptedException | URISyntaxException | IOException e) {
            throw new GradleException("Unable to download file", e);
        }
    }
}
