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
package io.micronaut.build.internal.openapi

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecOperations
import java.io.IOException
import java.nio.file.Files
import java.util.*
import javax.inject.Inject

/**
 * A task that simulates what the Gradle Micronaut plugin
 * would do. Must be used with the test entry point.
 */
abstract class OpenApiGeneratorTask : DefaultTask() {

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val openApiDefinition: RegularFileProperty

    @get:Input
    abstract val generatorKind: Property<String>

    @get:Input
    abstract val lang: Property<String>

    @get:Input
    abstract val generatedAnnotation: Property<Boolean>

    @get:Input
    abstract val ksp: Property<Boolean>

    @get:Input
    abstract val useOneOfInterfaces: Property<Boolean>

    @get:Input
    abstract val clientPath: Property<Boolean>

    @get:Input
    abstract val clientId: Property<String>

    @get:Input
    @get:Optional
    abstract val auth: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    val generatedSourcesDirectory: Provider<Directory>
        get() = outputDirectory.dir(
            if (lang.get().equals("JAVA", ignoreCase = true)) "src/main/java" else "src/main/kotlin"
        )

    @get:Internal
    val generatedTestSourcesDirectory: Provider<Directory>
        get() = outputDirectory.dir(
            if (lang.get().equals("JAVA", ignoreCase = true)) "src/test/java" else "src/test/kotlin"
        )

    @get:Input
    abstract val outputKinds: ListProperty<String>

    @get:Input
    abstract val parameterMappings: ListProperty<Map<String, String>>

    @get:Input
    abstract val responseBodyMappings: ListProperty<Map<String, String>>

    @get:Optional
    @get:Input
    abstract val nameMapping: MapProperty<String, String>

    @get:Optional
    @get:Input
    abstract val apiNamePrefix: Property<String>

    @get:Optional
    @get:Input
    abstract val apiNameSuffix: Property<String>

    @get:Optional
    @get:Input
    abstract val modelNamePrefix: Property<String>

    @get:Optional
    @get:Input
    abstract val modelNameSuffix: Property<String>

    @get:Inject
    internal abstract val execOperations: ExecOperations

    @Throws(IOException::class)
    @TaskAction
    fun execute() {
        val generatedSourcesDir = generatedSourcesDirectory.get().asFile
        val generatedTestSourcesDir = generatedTestSourcesDirectory.get().asFile
        val lang = lang.get()
        val taskClasspath = classpath

        Files.createDirectories(generatedSourcesDir.toPath())
        Files.createDirectories(generatedTestSourcesDir.toPath())
        project.logger.info("json: {}", parameterMappings.get())
        execOperations.javaexec {
            classpath = taskClasspath
            mainClass.set("io.micronaut.openapi.testsuite.GeneratorMain")
            args = listOf(
                generatorKind.get(),
                openApiDefinition.get().asFile.toURI().toString(),
                outputDirectory.get().asFile.absolutePath,
                outputKinds.get().joinToString(separator = ","),
                parameterMappings.get().toString(),
                responseBodyMappings.get().toString(),
                lang.uppercase(Locale.ENGLISH),
                generatedAnnotation.get().toString(),
                ksp.get().toString(),
                clientPath.get().toString(),
                useOneOfInterfaces.get().toString(),
                nameMapping.get().toString(),
                clientId.getOrElse(""),
                apiNamePrefix.getOrElse(""),
                apiNameSuffix.getOrElse(""),
                modelNamePrefix.getOrElse(""),
                modelNameSuffix.getOrElse(""),
                auth.getOrElse(false).toString(),
            )
        }
    }
}
