/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package io.micronaut.build.internal.openapi

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files

@CacheableTask
abstract class RemoteDownloadTask : DefaultTask() {

    @get:Input
    abstract val contentPath: Property<String>

    @get:Input
    abstract val baseUrl: Property<String>

    @get:Input
    abstract val outputPath: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val client = HttpClient.newHttpClient()
        var baseUrl = baseUrl.get()
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/"
        }
        try {
            val contentPath = contentPath.get()
            val uri = URI(baseUrl + contentPath)
            logger.lifecycle("Downloading {}", uri)
            val request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build()
            val outputFilePath = outputDirectory.get().asFile.toPath().resolve(outputPath.get())
            val parentDir = outputFilePath.parent
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir)
            }
            client.send(request, HttpResponse.BodyHandlers.ofFile(outputFilePath))
        } catch (e: Exception) {
            throw GradleException("Unable to download file", e)
        }
    }
}
