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
package io.micronaut.openapi.env;

import io.micronaut.context.ApplicationContextConfiguration;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.ContextUtils;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.micronaut.openapi.visitor.ConfigUtils.getProjectPath;
import static io.micronaut.openapi.visitor.FileUtils.CLASSPATH_SCHEME;
import static io.micronaut.openapi.visitor.FileUtils.FILE_SCHEME;
import static io.micronaut.openapi.visitor.FileUtils.PROJECT_SCHEME;
import static io.micronaut.openapi.visitor.FileUtils.normalizePath;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_ENVIRONMENT_ENABLED;
import static io.micronaut.openapi.visitor.StringUtil.COMMA;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;

/**
 * Specific environment for annotation processing level. Solve problem with access to resources
 * from project classpath.
 *
 * @since 4.5.0
 */
public class AnnProcessorEnvironment extends DefaultEnvironment {

    private final List<String> annotationProcessingConfigLocations;
    private String projectResourcesPath;
    private String projectDir = StringUtils.EMPTY_STRING;

    /**
     * Construct a new environment for the given configuration.
     *
     * @param configuration The configuration
     * @param context visitor context
     */
    public AnnProcessorEnvironment(ApplicationContextConfiguration configuration, VisitorContext context) {
        super(configuration, false);

        annotationProcessingConfigLocations = new ArrayList<>();

        boolean isEnabled = ContextUtils.get(MICRONAUT_ENVIRONMENT_ENABLED, Boolean.class, false, context);
        if (isEnabled) {
            Path projectPath = getProjectPath(context);
            if (projectPath != null) {
                projectDir = FILE_SCHEME + normalizePath(projectPath.toString());
                projectResourcesPath = projectDir + (projectDir.endsWith(SLASH) ? StringUtils.EMPTY_STRING : SLASH) + "src/main/resources/";
            }

            String configFileLocations = ContextUtils.getOptions(context).get(MICRONAUT_CONFIG_FILE_LOCATIONS);
            if (projectResourcesPath != null && StringUtils.isEmpty(configFileLocations)) {
                annotationProcessingConfigLocations.add(projectResourcesPath);
            } else if (StringUtils.isNotEmpty(configFileLocations)) {
                for (String configFileLocation : configFileLocations.split(COMMA)) {
                    if (!configFileLocation.startsWith(CLASSPATH_SCHEME) && !configFileLocation.startsWith(FILE_SCHEME) && !configFileLocation.startsWith(PROJECT_SCHEME)) {
                        throw new ConfigurationException("Unsupported config location format: " + configFileLocation);
                    }
                    if (configFileLocation.startsWith(PROJECT_SCHEME)) {
                        configFileLocation = configFileLocation.replace(PROJECT_SCHEME, projectDir);
                    }
                    annotationProcessingConfigLocations.add(configFileLocation);
                }
            }
        }
    }

    @Override
    public @NonNull Collection<PropertySourceLoader> getPropertySourceLoaders() {
        var loaders = new ArrayList<PropertySourceLoader>(2);
        loaders.add(new YamlPropertySourceLoader(false));
        loaders.add(new PropertiesPropertySourceLoader(false));
        return loaders;
    }
}
