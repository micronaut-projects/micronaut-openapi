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
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * A task which simulates what the Gradle Micronaut plugin
 * would do. Must be used with the test entry point.
 */
public abstract class OpenApiGeneratorTask extends DefaultTask {
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getOpenApiDefinition();

    @Input
    public abstract Property<String> getGeneratorKind();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Internal
    public Provider<Directory> getGeneratedSourcesDirectory() {
        return getOutputDirectory().dir("src/main/java");
    }

    @Internal
    public Provider<Directory> getGeneratedTestSourcesDirectory() {
        return getOutputDirectory().dir("src/test/java");
    }

    @Inject
    protected abstract ExecOperations getExecOperations();

    @TaskAction
    public void execute() throws IOException {
        var generatedSourcesDir = getGeneratedSourcesDirectory().get().getAsFile();
        var generatedTestSourcesDir = getGeneratedTestSourcesDirectory().get().getAsFile();
        Files.createDirectories(generatedSourcesDir.toPath());
        Files.createDirectories(generatedTestSourcesDir.toPath());
        getExecOperations().javaexec(javaexec -> {
            javaexec.setClasspath(getClasspath());
            javaexec.getMainClass().set("io.micronaut.openapi.testsuite.GeneratorMain");
            List<String> args = new ArrayList<>();
            args.add(getGeneratorKind().get());
            args.add(getOpenApiDefinition().get().getAsFile().toURI().toString());
            args.add(getOutputDirectory().get().getAsFile().getAbsolutePath());
            javaexec.args(args);
        });
    }
}
