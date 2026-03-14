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

import io.micronaut.context.ApplicationContextConfiguration;
import io.micronaut.context.env.ActiveEnvironment;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.io.file.DefaultFileSystemResourceLoader;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.env.DefaultEnvironment;
import org.jspecify.annotations.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /**
     * @param name The name to resolver property sources
     * @return The list of property sources
     */
    protected List<PropertySource> readPropertySourceList(String name) {
        var propertySources = new ArrayList<PropertySource>();
        for (String configLocation : annotationProcessingConfigLocations) {
            ResourceLoader resourceLoader;
            if (configLocation.equals("classpath:/")) {
                resourceLoader = this;
            } else if (configLocation.startsWith(CLASSPATH_SCHEME)) {
                if (this.resourceLoader instanceof DefaultClassPathResourceLoader defClassPathResourceLoader) {
                    resourceLoader = defClassPathResourceLoader.forBase(configLocation, false);
                } else {
                    resourceLoader = forBase(configLocation);
                }
            } else if (configLocation.startsWith(FILE_SCHEME)) {
                configLocation = configLocation.substring(5);
                Path configLocationPath = Path.of(configLocation);
                if (Files.exists(configLocationPath) && Files.isDirectory(configLocationPath) && Files.isReadable(configLocationPath)) {
                    resourceLoader = new DefaultFileSystemResourceLoader(configLocationPath);
                } else {
                    continue; // Skip not existing config location
                }
            } else {
                throw new ConfigurationException("Unsupported config location format: " + configLocation);
            }
            readPropSourceList(name, resourceLoader, propertySources);
        }
        return propertySources;
    }

    private void readPropSourceList(String name, ResourceLoader resourceLoader, List<PropertySource> propertySources) {
        Collection<PropertySourceLoader> propertySourceLoaders = getPropertySourceLoaders();
        if (propertySourceLoaders.isEmpty()) {
            var propertySourceLoader = new PropertiesPropertySourceLoader(false);
            loadPropSourceFromLoader(name, propertySourceLoader, propertySources, resourceLoader);
        } else {
            for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
                loadPropSourceFromLoader(name, propertySourceLoader, propertySources, resourceLoader);
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

    private void loadPropSourceFromLoader(String name, PropertySourceLoader propertySourceLoader, List<PropertySource> propertySources, ResourceLoader resourceLoader) {
        Optional<PropertySource> defaultPropertySource = propertySourceLoader.load(name, resourceLoader);
        defaultPropertySource.ifPresent(propertySources::add);
        Set<String> activeNames = getActiveNames();
        int i = 0;
        for (String activeName : activeNames) {
            Optional<PropertySource> propertySource = propertySourceLoader.loadEnv(name, resourceLoader, ActiveEnvironment.of(activeName, i));
            propertySource.ifPresent(propertySources::add);
            i++;
        }
    }
}
