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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.micronaut.context.ApplicationContextConfiguration;
import io.micronaut.context.env.ActiveEnvironment;
import io.micronaut.context.env.CachedEnvironment;
import io.micronaut.context.env.DefaultEnvironment;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.io.file.DefaultFileSystemResourceLoader;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;

import static io.micronaut.openapi.visitor.ConfigUtils.getProjectPath;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_CONFIG_FILE_LOCATIONS;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_ENVIRONMENT_ENABLED;

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
                projectDir = "file:" + projectPath.toString().replaceAll("\\\\", "/");
                projectResourcesPath = projectDir + (projectDir.endsWith("/") ? StringUtils.EMPTY_STRING : "/") + "src/main/resources/";
            }

            String configFileLocations = ContextUtils.getOptions(context).get(MICRONAUT_CONFIG_FILE_LOCATIONS);
            if (projectResourcesPath != null && StringUtils.isEmpty(configFileLocations)) {
                annotationProcessingConfigLocations.add(projectResourcesPath);
            } else if (StringUtils.isNotEmpty(configFileLocations)) {
                for (String configFileLocation : configFileLocations.split(",")) {
                    if (!configFileLocation.startsWith("classpath") && !configFileLocation.startsWith("file") && !configFileLocation.startsWith("project")) {
                        throw new ConfigurationException("Unsupported config location format: " + configFileLocation);
                    }
                    if (configFileLocation.startsWith("project")) {
                        configFileLocation = configFileLocation.replace("project:", projectDir);
                    }
                    annotationProcessingConfigLocations.add(configFileLocation);
                }
            }
        }
    }

    /**
     * @param name The name to read property sources
     */
    @Override
    protected void readPropertySources(String name) {
        refreshablePropertySources.clear();

        List<PropertySource> propertySources = readPropertySourceList(name);
        addDefaultPropertySources(propertySources);
        String propertySourcesSystemProperty = CachedEnvironment.getProperty(Environment.PROPERTY_SOURCES_KEY);
        if (propertySourcesSystemProperty != null) {
            if (propertySourcesSystemProperty.startsWith("project")) {
                propertySourcesSystemProperty = propertySourcesSystemProperty.replaceAll("project:", projectDir);
            }
            propertySources.addAll(readPropertySourceListFromFiles(propertySourcesSystemProperty));
        }
        String propertySourcesEnv = readPropertySourceListKeyFromEnvironment();
        if (propertySourcesEnv != null) {
            if (propertySourcesEnv.startsWith("project")) {
                propertySourcesEnv = propertySourcesEnv.replaceAll("project:", projectDir);
            }
            propertySources.addAll(readPropertySourceListFromFiles(propertySourcesEnv));
        }
        refreshablePropertySources.addAll(propertySources);
        readConstantPropertySources(name, propertySources);

        propertySources.addAll(this.propertySources.values());
        OrderUtil.sort(propertySources);
        for (PropertySource propertySource : propertySources) {
            processPropertySource(propertySource, propertySource.getConvention());
        }
    }

    private void readConstantPropertySources(String name, List<PropertySource> propertySources) {
        var propertySourceNames = new HashSet<String>(getActiveNames().size() + 1);
        propertySourceNames.add(name);
        for (var activeName : getActiveNames()) {
            propertySourceNames.add(name + '-' + activeName);
        }
        for (var constPropertySource : getConstantPropertySources()) {
            if (propertySourceNames.contains(constPropertySource.getName())) {
                propertySources.add(constPropertySource);
            }
        }
    }

    /**
     * @param name The name to resolver property sources
     *
     * @return The list of property sources
     */
    @Override
    protected List<PropertySource> readPropertySourceList(String name) {
        List<PropertySource> propertySources = new ArrayList<>();
        for (String configLocation : annotationProcessingConfigLocations) {
            ResourceLoader resourceLoader;
            if (configLocation.equals("classpath:/")) {
                resourceLoader = this;
            } else if (configLocation.startsWith("classpath:")) {
                if (this.resourceLoader instanceof DefaultClassPathResourceLoader defClassPathResourceLoader) {
                    resourceLoader = defClassPathResourceLoader.forBase(configLocation, false);
                } else {
                    resourceLoader = forBase(configLocation);
                }
            } else if (configLocation.startsWith("file:")) {
                configLocation = configLocation.substring(5);
                Path configLocationPath = Paths.get(configLocation);
                if (Files.exists(configLocationPath) && Files.isDirectory(configLocationPath) && Files.isReadable(configLocationPath)) {
                    resourceLoader = new DefaultFileSystemResourceLoader(configLocationPath);
                } else {
                    continue; // Skip not existing config location
                }
            } else {
                throw new ConfigurationException("Unsupported config location format: " + configLocation);
            }
            readPropertySourceList(name, resourceLoader, propertySources);
        }
        return propertySources;
    }

    private void readPropertySourceList(String name, ResourceLoader resourceLoader, List<PropertySource> propertySources) {
        Collection<PropertySourceLoader> propertySourceLoaders = getPropertySourceLoaders();
        if (propertySourceLoaders.isEmpty()) {
            PropertiesPropertySourceLoader propertySourceLoader = new PropertiesPropertySourceLoader(false);
            loadPropertySourceFromLoader(name, propertySourceLoader, propertySources, resourceLoader);
        } else {
            for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
                loadPropertySourceFromLoader(name, propertySourceLoader, propertySources, resourceLoader);
            }
        }
    }

    @Override
    public Collection<PropertySourceLoader> getPropertySourceLoaders() {
        List<PropertySourceLoader> loaders = new ArrayList<>(2);
        loaders.add(new YamlPropertySourceLoader(false));
        loaders.add(new PropertiesPropertySourceLoader(false));
        return loaders;
    }

    private void loadPropertySourceFromLoader(String name, PropertySourceLoader propertySourceLoader, List<PropertySource> propertySources, ResourceLoader resourceLoader) {
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
