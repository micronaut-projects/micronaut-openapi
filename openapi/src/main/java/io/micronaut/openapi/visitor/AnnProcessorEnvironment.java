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
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.MutableConversionService;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.io.file.DefaultFileSystemResourceLoader;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.core.util.StringUtils;
import io.micronaut.context.env.PropertyPlaceholderResolver;
import io.micronaut.inject.BeanConfiguration;
import io.micronaut.inject.visitor.VisitorContext;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

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

    private final List<String> annotationProcessingConfigLocations = new ArrayList<>();
    private String projectResourcesPath;
    private String projectDir = StringUtils.EMPTY_STRING;
    private final Set<String> activeNames = new HashSet<>();
    private final Map<String, PropertySource> propertySources = new ConcurrentHashMap<>();
    private final List<PropertySource> refreshablePropertySources = new ArrayList<>();
    private final ResourceLoader resourceLoader;
    private final MutableConversionService conversionService = new io.micronaut.core.convert.DefaultMutableConversionService();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean reading = new AtomicBoolean(false);
    private final Collection<String> packages = new HashSet<>();

    /**
     * Construct a new environment for the given configuration.
     *
     * @param configuration The configuration
     * @param context visitor context
     */
    public AnnProcessorEnvironment(ApplicationContextConfiguration configuration, VisitorContext context) {
        this.resourceLoader = configuration.getResourceLoader();
        activeNames.addAll(configuration.getEnvironments());

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
    public @NonNull Environment start() {
        if (running.compareAndSet(false, true)) {
            readProperties();
        }
        return this;
    }

    private void readProperties() {
        if (reading.compareAndSet(false, true)) {
            loadProperties();
            reading.set(false);
        }
    }

    private void loadProperties() {
        refreshablePropertySources.clear();
        List<PropertySource> propertySourcesList = readPropertySourceList("application");
        addDefaultPropertySources(propertySourcesList);

        String propertySourcesSystemProperty = System.getProperty(Environment.PROPERTY_SOURCES_KEY);
        if (propertySourcesSystemProperty != null) {
            if (propertySourcesSystemProperty.startsWith(PROJECT_SCHEME)) {
                propertySourcesSystemProperty = propertySourcesSystemProperty.replaceAll(PROJECT_SCHEME, projectDir);
            }
            propertySourcesList.addAll(readPropertySourceListFromFiles(propertySourcesSystemProperty));
        }
        String propertySourcesEnv = System.getenv("MICRONAUT_CONFIG_FILES");
        if (propertySourcesEnv != null) {
            if (propertySourcesEnv.startsWith(PROJECT_SCHEME)) {
                propertySourcesEnv = propertySourcesEnv.replace(PROJECT_SCHEME, projectDir);
            }
            propertySourcesList.addAll(readPropertySourceListFromFiles(propertySourcesEnv));
        }
        refreshablePropertySources.addAll(propertySourcesList);

        readConstPropertySources("application", propertySourcesList);

        propertySourcesList.addAll(propertySources.values());
        OrderUtil.sort(propertySourcesList);
        for (PropertySource ps : propertySourcesList) {
            processPropertySource(ps);
        }
    }

    private void readConstPropertySources(String name, List<PropertySource> propertySourcesList) {
        var propertySourceNames = new HashSet<String>(activeNames.size() + 1);
        propertySourceNames.add(name);
        for (var activeName : activeNames) {
            propertySourceNames.add(name + '-' + activeName);
        }
    }

    private void processPropertySource(PropertySource propertySource) {

    }

    private List<PropertySource> readPropertySourceListFromFiles(String files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> fileList = List.of(files.split(","));
        List<PropertySource> sources = new ArrayList<>();
        int order = 100;
        for (String file : fileList) {
            Optional<Map<String, Object>> props = loadFromFile(file);
            if (props.isPresent()) {
                sources.add(PropertySource.of(file, props.get(), order++));
            }
        }
        return sources;
    }

    private Optional<Map<String, Object>> loadFromFile(String file) {
        try {
            Path filePath = Path.of(file);
            ResourceLoader fileLoader = new DefaultFileSystemResourceLoader(filePath.getParent());
            io.micronaut.context.env.PropertiesPropertySourceLoader loader = new io.micronaut.context.env.PropertiesPropertySourceLoader();
            Optional<PropertySource> ps = loader.load(NameUtils.filename(file), fileLoader);
            if (ps.isPresent()) {
                return Optional.of(getSourceAsMap(ps.get()));
            }
        } catch (Exception e) {
            // ignore
        }
        return Optional.empty();
    }

    private Map<String, Object> getSourceAsMap(PropertySource source) {
        Map<String, Object> map = new HashMap<>();
        for (String key : source) {
            Object value = source.get(key);
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    private void addDefaultPropertySources(List<PropertySource> propertySourcesList) {
        String sysName = "system-properties";
        if (propertySources.get(sysName) == null) {
            PropertySource sysProps = PropertySource.of(sysName, (Map<String, Object>) (Map) System.getProperties());
            propertySourcesList.add(sysProps);
        }
        String envName = "system-environment";
        if (propertySources.get(envName) == null) {
            PropertySource envProps = PropertySource.of(envName, (Map<String, Object>) (Map) System.getenv());
            propertySourcesList.add(envProps);
        }
    }

    @Override
    public @NonNull Map<String, Object> getProperties(@NonNull String name, @NonNull StringConvention convention) {
        Map<String, Object> result = new HashMap<>();
        for (PropertySource source : propertySources.values()) {
            Map<String, Object> props = getSourceAsMap(source);
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(name)) {
                    result.put(key, entry.getValue());
                }
            }
        }
        return result;
    }

    @Override
    public <T> Optional<T> getProperty(@NonNull String name, @NonNull Class<T> requiredType) {
        for (PropertySource source : propertySources.values()) {
            Object value = source.get(name);
            if (value != null) {
                if (requiredType.isInstance(value)) {
                    return Optional.of(requiredType.cast(value));
                } else {
                    Optional<T> converted = conversionService.convert(value, requiredType);
                    if (converted.isPresent()) {
                        return converted;
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getProperty(@NonNull String name, @NonNull ArgumentConversionContext<T> context) {
        Optional<?> value = getProperty(name, Object.class);
        if (value.isPresent()) {
            return conversionService.convert(value.get(), context);
        }
        return Optional.empty();
    }

    final @NonNull Map<String, Object> getAllProperties(@Nullable String name, @Nullable StringConvention keyFormat) {
        Map<String, Object> all = new HashMap<>();
        for (PropertySource source : propertySources.values()) {
            all.putAll(getSourceAsMap(source));
        }
        return all;
    }

    @Override
    public boolean containsProperty(@NonNull String name) {
        return propertySources.values().stream().anyMatch(ps -> ps.get(name) != null);
    }

    @Override
    public boolean containsProperties(@NonNull String name) {
        return containsProperty(name);
    }

    @Override
    public @NonNull Collection<List<String>> getPropertyPathMatches(@NonNull String name) {
        return Collections.emptyList();
    }

    private List<PropertySource> readPropertySourceList(String name) {
        List<PropertySource> sources = new ArrayList<>();
        for (String configLocation : annotationProcessingConfigLocations) {
            ResourceLoader rl;
            if ("classpath:/".equals(configLocation)) {
                rl = resourceLoader;
            } else if (configLocation.startsWith(CLASSPATH_SCHEME)) {
                rl = resourceLoader.forBase(configLocation);
            } else if (configLocation.startsWith(FILE_SCHEME)) {
                configLocation = configLocation.substring(5);
                Path path = Path.of(configLocation);
                if (Files.exists(path) && Files.isDirectory(path) && Files.isReadable(path)) {
                    rl = new DefaultFileSystemResourceLoader(path);
                } else {
                    continue;
                }
            } else {
                throw new ConfigurationException("Unsupported config location format: " + configLocation);
            }
            readPropSourceList(name, rl, sources);
        }
        return sources;
    }

    private void readPropSourceList(String name, ResourceLoader rl, List<PropertySource> sources) {
        io.micronaut.context.env.PropertiesPropertySourceLoader loader = new io.micronaut.context.env.PropertiesPropertySourceLoader();
        loadPropSourceFromLoader(name, loader, sources, rl);
        io.micronaut.context.env.yaml.YamlPropertySourceLoader yamlLoader = new io.micronaut.context.env.yaml.YamlPropertySourceLoader();
        loadPropSourceFromLoader(name, yamlLoader, sources, rl);
    }

    @Override
    public @NonNull Collection<PropertySourceLoader> getPropertySourceLoaders() {
        return Collections.emptyList();
    }

    private void loadPropSourceFromLoader(String name, PropertySourceLoader loader, List<PropertySource> sources, ResourceLoader rl) {
        Optional<PropertySource> defaultPs = loader.load(name, rl);
        defaultPs.ifPresent(sources::add);
        int i = 0;
        for (String activeName : activeNames) {
            Optional<PropertySource> envPs = loader.loadEnv(name, rl, ActiveEnvironment.of(activeName, i++));
            envPs.ifPresent(sources::add);
        }
    }

    @Override
    public @NonNull Environment stop() {
        running.set(false);
        reading.set(false);
        refreshablePropertySources.clear();
        propertySources.clear();
        return this;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public @NonNull Set<String> getActiveNames() {
        return Collections.unmodifiableSet(activeNames);
    }

    @Override
    public @NonNull Collection<PropertySource> getPropertySources() {
        return Collections.unmodifiableCollection(propertySources.values());
    }

    @Override
    public Optional<InputStream> getResourceAsStream(@NonNull String name) {
        return resourceLoader.getResourceAsStream(name);
    }

    @Override
    public Optional<URL> getResource(@NonNull String name) {
        return resourceLoader.getResource(name);
    }

    @Override
    public Stream<URL> getResources(@NonNull String name) {
        return resourceLoader.getResources(name);
    }

    @Override
    public boolean supportsPrefix(@NonNull String name) {
        return resourceLoader.supportsPrefix(name);
    }

    @Override
    public @NonNull ResourceLoader forBase(@NonNull String name) {
        return resourceLoader.forBase(name);
    }

    @Override
    public Stream<Class<?>> scan(Class<? extends java.lang.annotation.Annotation> annotation) {
        return Stream.empty();
    }

    @Override
    public Stream<Class<?>> scan(Class<? extends java.lang.annotation.Annotation> annotation, String... packages) {
        return Stream.empty();
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    public MutableConversionService getConversionService() {
        return conversionService;
    }

    @Override
    public boolean isActive(BeanConfiguration configuration) {
        return true;
    }

    @Override
    public @NonNull Environment addPropertySource(@NonNull PropertySource propertySource) {
        propertySources.put(propertySource.getName(), propertySource);
        if (running.get()) {
            refreshablePropertySources.add(propertySource);
        }
        return this;
    }

    @Override
    public @NonNull Environment removePropertySource(@NonNull PropertySource propertySource) {
        propertySources.remove(propertySource.getName());
        refreshablePropertySources.remove(propertySource);
        return this;
    }

    @Override
    public @NonNull Environment addPackage(@NonNull String pkg) {
        packages.add(pkg);
        return this;
    }

    @Override
    public @NonNull Collection<String> getPackages() {
        return Collections.unmodifiableCollection(packages);
    }

    @Override
    public @NonNull Environment addConfigurationExcludes(@NonNull String... names) {
        return this;
    }

    @Override
    public @NonNull Environment addConfigurationIncludes(@NonNull String... names) {
        return this;
    }

    @Override
    public @NonNull Map<String, Object> refreshAndDiff() {
        return Collections.emptyMap();
    }

    @Override
    public @NonNull Environment refresh() {
        loadProperties();
        return this;
    }

    @Override
    public @NonNull PropertyPlaceholderResolver getPlaceholderResolver() {
        return new PropertyPlaceholderResolver() {
            @Override
            public Optional<String> resolvePlaceholders(String str) {
                return Optional.of(str); // No resolution for now
            }
        };
    }

    @Override
    public <T> T getRequiredProperty(@NonNull String name, @NonNull Class<T> requiredType) {
        return getProperty(name, requiredType).orElseThrow(() -> new ConfigurationException("Required property [" + name + "] not found"));
    }
}
