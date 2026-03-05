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
import io.micronaut.context.env.*;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.io.file.DefaultFileSystemResourceLoader;
import io.micronaut.core.io.file.FileSystemResourceLoader;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
public class AnnProcessorEnvironment implements Environment {

    private final Environment delegate;
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
        // create the standard DefaultEnvironment via the Environment factory
        this.delegate = Environment.create(configuration);
        this.annotationProcessingConfigLocations = new ArrayList<>();

        boolean isEnabled = ContextUtils.get(MICRONAUT_ENVIRONMENT_ENABLED, Boolean.class, true, context);
        if (isEnabled) {
            Path projectPath = getProjectPath(context);
            if (projectPath != null) {
                projectDir = FILE_SCHEME + normalizePath(projectPath.toString());
                projectResourcesPath = projectDir + (projectDir.endsWith(SLASH) ? StringUtils.EMPTY_STRING : SLASH) + "src/main/resources/";
                if (Utils.isTestMode()) {
                    annotationProcessingConfigLocations.add(projectDir + (projectDir.endsWith(SLASH) ? StringUtils.EMPTY_STRING : SLASH) + "src/test/resources/");
                }
            }

            // Support both annotation-processor options and system properties (tests frequently use System.setProperty).
            //
            // IMPORTANT: do NOT call ConfigUtils.getConfigProperty(...) here because it may call ConfigUtils.getEnv(...)
            // which constructs another AnnProcessorEnvironment -> recursion during visitor startup.
            String configFileLocations = ContextUtils.getOptions(context).get(MICRONAUT_CONFIG_FILE_LOCATIONS);
            if (StringUtils.isEmpty(configFileLocations)) {
                configFileLocations = CachedEnvironment.getProperty(MICRONAUT_CONFIG_FILE_LOCATIONS);
            }
            if (StringUtils.isEmpty(configFileLocations)) {
                configFileLocations = System.getProperty(MICRONAUT_CONFIG_FILE_LOCATIONS);
            }

            if (projectResourcesPath != null && StringUtils.isEmpty(configFileLocations)) {
                annotationProcessingConfigLocations.add(projectResourcesPath);
            } else if (StringUtils.isNotEmpty(configFileLocations)) {
                for (String configFileLocation : configFileLocations.split(COMMA)) {
                    if (!configFileLocation.startsWith(CLASSPATH_SCHEME) &&
                        !configFileLocation.startsWith(FILE_SCHEME) &&
                        !configFileLocation.startsWith(PROJECT_SCHEME)) {
                        throw new ConfigurationException("Unsupported config location format: " + configFileLocation);
                    }
                    if (configFileLocation.startsWith(PROJECT_SCHEME)) {
                        configFileLocation = configFileLocation.replace(PROJECT_SCHEME, projectDir);
                    }
                    annotationProcessingConfigLocations.add(configFileLocation);
                }
            }
        }

        // Load property sources discovered for annotation-processing locations and add them to the delegate
        if (!annotationProcessingConfigLocations.isEmpty()) {
            // Micronaut convention: configuration base name is "application".
            // ApplicationContextConfiguration#getApplicationName can be null/blank during annotation processing,
            // which would make config loading non-deterministic.
            List<PropertySource> propertySources = readPropertySourceList("application");
            // add loaded property sources to delegate environment
            for (PropertySource ps : propertySources) {
                delegate.addPropertySource(ps);
            }
        }

        String propertySourcesSystemProperty = CachedEnvironment.getProperty(Environment.PROPERTY_SOURCES_KEY);
        if (propertySourcesSystemProperty != null && !propertySourcesSystemProperty.isBlank()) {
            if (propertySourcesSystemProperty.startsWith(PROJECT_SCHEME)) {
                propertySourcesSystemProperty = propertySourcesSystemProperty.replaceAll(PROJECT_SCHEME, projectDir);
            }
            for (PropertySource ps : readPropertySourceListFromFiles(propertySourcesSystemProperty)) {
                delegate.addPropertySource(ps);
            }
        }
        String propertySourcesEnv = CachedEnvironment.getenv("MICRONAUT_CONFIG_FILES");
        if (propertySourcesEnv != null && !propertySourcesEnv.isBlank()) {
            if (propertySourcesEnv.startsWith(PROJECT_SCHEME)) {
                propertySourcesEnv = propertySourcesEnv.replace(PROJECT_SCHEME, projectDir);
            }
            for (PropertySource ps : readPropertySourceListFromFiles(propertySourcesEnv)) {
                delegate.addPropertySource(ps);
            }
        }
    }

    // --------------------------
    // Property source discovery
    // --------------------------
    protected List<PropertySource> readPropertySourceList(String name) {
        var propertySources = new ArrayList<PropertySource>();
        for (String configLocation : annotationProcessingConfigLocations) {
            ResourceLoader resourceLoader;
            if (configLocation.equals("classpath:/")) {
                resourceLoader = (ResourceLoader) delegate;
            } else if (configLocation.startsWith(CLASSPATH_SCHEME)) {
                resourceLoader = delegate.forBase(configLocation);
            } else if (configLocation.startsWith(FILE_SCHEME)) {
                String path = configLocation.substring(FILE_SCHEME.length());
                Path configLocationPath = Path.of(path);
                if (Files.exists(configLocationPath) && Files.isDirectory(configLocationPath) && Files.isReadable(configLocationPath)) {
                    resourceLoader = new DefaultFileSystemResourceLoader(configLocationPath);
                } else {
                    continue;
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
        if (propertySourceLoaders == null || propertySourceLoaders.isEmpty()) {
            loadPropSourceFromLoader(name, new PropertiesPropertySourceLoader(false), propertySources, resourceLoader);
            loadPropSourceFromLoader(name, new YamlPropertySourceLoader(), propertySources, resourceLoader);
        } else {
            boolean hasYaml = false;
            for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
                loadPropSourceFromLoader(name, propertySourceLoader, propertySources, resourceLoader);
                if (propertySourceLoader instanceof YamlPropertySourceLoader) {
                    hasYaml = true;
                }
            }
            if (!hasYaml) {
                loadPropSourceFromLoader(name, new YamlPropertySourceLoader(), propertySources, resourceLoader);
            }
        }
    }

    private void loadPropSourceFromLoader(String name,
                                          PropertySourceLoader propertySourceLoader,
                                          List<PropertySource> propertySources,
                                          ResourceLoader resourceLoader) {
        Optional<PropertySource> defaultPropertySource = propertySourceLoader.load(name, resourceLoader);
        defaultPropertySource.ifPresent(propertySources::add);
        Set<String> activeNames = delegate.getActiveNames();
        int i = 0;
        for (String activeName : activeNames) {
            Optional<PropertySource> propertySource = propertySourceLoader.loadEnv(name, resourceLoader, ActiveEnvironment.of(activeName, i));
            propertySource.ifPresent(propertySources::add);
            i++;
        }
    }

    private List<PropertySource> readPropertySourceListFromFiles(String files) {
        if (files == null || files.isBlank()) {
            return List.of();
        }
        String[] parts = files.split(",");
        if (parts.length == 0) {
            return List.of();
        }
        List<PropertySource> result = new ArrayList<>();
        Collection<PropertySourceLoader> loaders = getPropertySourceLoaders();
        for (String raw : parts) {
            String filePath = raw.trim();
            if (filePath.isEmpty()) continue;
            String extension = io.micronaut.core.naming.NameUtils.extension(filePath);
            PropertySourceLoader matched = null;
            for (PropertySourceLoader l : loaders) {
                if (l.getExtensions().contains(extension)) {
                    matched = l;
                    break;
                }
            }
            if (matched == null) {
                throw new ConfigurationException("Unsupported properties file format while reading " + filePath);
            }
            Optional<Map<String, Object>> read = readPropertiesFromLoader(io.micronaut.core.naming.NameUtils.filename(filePath), filePath, matched);
            read.ifPresent(m -> result.add(PropertySource.of(filePath, m)));
        }
        return result;
    }

    private Optional<Map<String, Object>> readPropertiesFromLoader(String fileName, String filePath, PropertySourceLoader propertySourceLoader) {
        ResourceLoader loader = new ResourceResolver().getSupportingLoader(filePath)
            .orElse(FileSystemResourceLoader.defaultLoader());
        try {
            Optional<InputStream> inputStream = loader.getResourceAsStream(filePath);
            if (inputStream.isPresent()) {
                return Optional.of(propertySourceLoader.read(fileName, inputStream.get()));
            } else {
                throw new ConfigurationException("Failed to read configuration file: " + filePath);
            }
        } catch (IOException e) {
            throw new ConfigurationException("Unsupported properties file: " + fileName);
        }
    }

    // --------------------------
    // Delegate Environment API
    // --------------------------

    @Override
    public Set<String> getActiveNames() {
        return delegate.getActiveNames();
    }

    @Override
    public Collection<PropertySource> getPropertySources() {
        return delegate.getPropertySources();
    }

    @Override
    public Environment addPropertySource(PropertySource propertySource) {
        delegate.addPropertySource(propertySource);
        return this;
    }

    @Override
    public Environment removePropertySource(PropertySource propertySource) {
        delegate.removePropertySource(propertySource);
        return this;
    }

    @Override
    public Environment addPackage(String pkg) {
        delegate.addPackage(pkg);
        return this;
    }

    @Override
    public Environment addConfigurationExcludes(String... names) {
        delegate.addConfigurationExcludes(names);
        return this;
    }

    @Override
    public Environment addConfigurationIncludes(String... names) {
        delegate.addConfigurationIncludes(names);
        return this;
    }

    @Override
    public Collection<String> getPackages() {
        return delegate.getPackages();
    }

    @Override
    public PropertyPlaceholderResolver getPlaceholderResolver() {
        return delegate.getPlaceholderResolver();
    }

    @Override
    public Map<String, Object> refreshAndDiff() {
        return delegate.refreshAndDiff();
    }

    @Override
    public Environment refresh() {
        delegate.refresh();
        return this;
    }

    @Override
    public Optional<java.io.InputStream> getResourceAsStream(String path) {
        return delegate.getResourceAsStream(path);
    }

    @Override
    public Optional<URL> getResource(String path) {
        return delegate.getResource(path);
    }

    @Override
    public java.util.stream.Stream<URL> getResources(String path) {
        return delegate.getResources(path);
    }

    @Override
    public boolean supportsPrefix(String path) {
        return delegate.supportsPrefix(path);
    }

    @Override
    public ResourceLoader forBase(String basePath) {
        return delegate.forBase(basePath);
    }

    @Override
    public boolean isPresent(String className) {
        return delegate.isPresent(className);
    }

    @Override
    public java.util.stream.Stream<Class<?>> scan(Class<? extends java.lang.annotation.Annotation> annotation) {
        return delegate.scan(annotation);
    }

    @Override
    public java.util.stream.Stream<Class<?>> scan(Class<? extends java.lang.annotation.Annotation> annotation, String... packages) {
        return delegate.scan(annotation, packages);
    }

    @Override
    public java.lang.ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }

    @Override
    public boolean isActive(io.micronaut.inject.BeanConfiguration configuration) {
        return delegate.isActive(configuration);
    }

    @Override
    public Environment start() {
        delegate.start();
        return this;
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    @Override
    public Environment stop() {
        delegate.stop();
        return this;
    }

    @Override
    public java.util.Optional<PropertyEntry> getPropertyEntry(String name) {
        try {
            return delegate.getPropertyEntry(name);
        } catch (NoSuchMethodError e) {
            return Optional.empty();
        }
    }

    @Override
    public java.util.Collection<PropertySourceLoader> getPropertySourceLoaders() {
        return delegate.getPropertySourceLoaders();
    }

    @Override
    public io.micronaut.core.convert.MutableConversionService getConversionService() {
        return delegate.getConversionService();
    }


    @Override
    public boolean containsProperty(String name) {
        return delegate.containsProperty(name);
    }

    @Override
    public boolean containsProperties(String name) {
        return delegate.containsProperties(name);
    }

    @Override
    public <T> Optional<T> getProperty(String name, Class<T> requiredType) {
        return delegate.getProperty(name, requiredType);
    }

    @Override
    public <T> T getRequiredProperty(String name, Class<T> requiredType) {
        return delegate.getRequiredProperty(name, requiredType);
    }

    @Override
    public <T> Optional<T> getProperty(String name, ArgumentConversionContext<T> conversionContext) {
        return delegate.getProperty(name, conversionContext);
    }

    @Override
    public Collection<List<String>> getPropertyPathMatches(String pathPattern) {
        return delegate.getPropertyPathMatches(pathPattern);
    }

    /**
     * Returns all property key-value pairs whose key starts with the given prefix,
     * reconstructing dotted class-name keys that YAML nesting would otherwise obscure.
     * This is needed because Micronaut treats dots in YAML map keys as nesting separators,
     * so keys like "io.micronaut.Foo" become nested maps rather than flat string keys.
     */
    public Map<String, String> getFlatProperties(String prefix) {
        String dotPrefix = prefix + ".";
        var result = new HashMap<String, String>();
        for (PropertySource ps : delegate.getPropertySources()) {
            for (String key : ps) {
                if (key.startsWith(dotPrefix)) {
                    Object value = ps.get(key);
                    if (value != null) {
                        result.put(key.substring(dotPrefix.length()), value.toString());
                    }
                }
            }
        }
        return result;
    }
}
