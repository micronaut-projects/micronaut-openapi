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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.GenericArgument;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.inject.writer.GeneratedFile;
import io.micronaut.openapi.visitor.group.GroupProperties;
import io.micronaut.openapi.visitor.management.EndpointProperties;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.micronaut.inject.visitor.VisitorContext.MICRONAUT_PROCESSING_PROJECT_DIR;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_CLASSPATH_OUTPUT;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_GENERATED_FILE;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.PREFIX_DUMMY_FILE;
import static io.micronaut.openapi.visitor.StringUtil.WILDCARD;

/**
 * Convert utilities methods.
 *
 * @since 4.5.0
 */
@Internal
public final class ContextUtils {

    public static final Argument<List<Tag>> TAGS_LIST_ARGUMENT = new GenericArgument<>() {
    };
    public static final Argument<List<Server>> SERVERS_LIST_ARGUMENT = new GenericArgument<>() {
    };
    public static final Argument<Map<String, Object>> EXTENSIONS_MAP_ARGUMENT = new GenericArgument<>() {
    };
    public static final Argument<List<Pair<String, String>>> EXPANDABLE_PROPERTIES_ARGUMENT = new GenericArgument<>() {
    };
    public static final Argument<Map<String, ConfigUtils.SchemaDecorator>> ARGUMENT_SCHEMA_DECORATORS_MAP = new GenericArgument<>() {
    };
    public static final Argument<Map<String, ConfigUtils.CustomSchema>> ARGUMENT_CUSTOM_SCHEMA_MAP = new GenericArgument<>() {
    };
    public static final Argument<Map<String, GroupProperties>> ARGUMENT_GROUP_PROPERTIES_MAP = new GenericArgument<>() {
    };
    public static final Argument<Map<String, EndpointProperties>> ARGUMENT_ENDPOINT_PROPERTIES_MAP = new GenericArgument<>() {
    };

    private ContextUtils() {
    }

    public static Integer getVisitedElements(VisitorContext context) {
        Integer visitedElements = get(Utils.ATTR_VISITED_ELEMENTS, Integer.class, null, context);
        if (visitedElements == null) {
            visitedElements = 0;
            put(Utils.ATTR_VISITED_ELEMENTS, visitedElements, context);
        }
        return visitedElements;
    }

    @Nullable
    public static Path getClassesOutputPath(VisitorContext context) {

        if (context == null) {
            return null;
        }

        var outputPath = get(MICRONAUT_INTERNAL_CLASSPATH_OUTPUT, Path.class, null, context);
        if (outputPath != null) {
            return outputPath;
        }
        var generatedFile = visitMetaInfFile(PREFIX_DUMMY_FILE, context);
        if (generatedFile != null) {
            return calcClassesOutputPath(generatedFile, context);
        }
        return null;
    }

    public static Path calcClassesOutputPath(GeneratedFile generatedFile, VisitorContext context) {
        // trying to calculate project directory path, if needed, and we can
        // if it's absolute path, we can't calculate project directory path
        try {
            if (!contains(MICRONAUT_INTERNAL_CLASSPATH_OUTPUT, context)) {

                var uri = generatedFile.toURI();
                // happens in tests 'mem:///CLASS_OUTPUT/META-INF/swagger/swagger.yml'
                if (uri.getScheme() != null && !uri.getScheme().equals("mem")) {
                    var filePath = Path.of(uri).normalize();
                    while (filePath != null) {
                        Path fileName = filePath.getFileName();
                        if (fileName != null && "META-INF".equals(fileName.toString())) {
                            put(MICRONAUT_INTERNAL_CLASSPATH_OUTPUT, filePath, context);
                            return filePath;
                        }
                        filePath = filePath.getParent();
                    }
                    return null;
                }
            } else {
                return get(MICRONAUT_INTERNAL_CLASSPATH_OUTPUT, Path.class, null, context);
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    public static Path calcProjectPath(GeneratedFile generatedFile, VisitorContext context) {
        // trying to calculate project directory path, if needed, and we can
        // if it's absolute path, we can't calculate project directory path
        try {
            if (!contains(MICRONAUT_PROCESSING_PROJECT_DIR, context)) {
                Path projectDir;
                URI uri = generatedFile.toURI();
                // happens in tests 'mem:///CLASS_OUTPUT/dummy'
                if (uri.getScheme() != null && !uri.getScheme().equals("mem")) {
                    // assume files are generated in 'build' or 'target' directories
                    Path filePath = Path.of(uri).normalize();
                    while (filePath != null) {
                        Path dummyFileName = filePath.getFileName();
                        if (dummyFileName != null && ("build".equals(dummyFileName.toString()) || "target".equals(dummyFileName.toString()))) {
                            projectDir = filePath.getParent();
                            put(MICRONAUT_PROCESSING_PROJECT_DIR, projectDir, context);
                            return projectDir;
                        }
                        filePath = filePath.getParent();
                    }
                }
            } else {
                return get(MICRONAUT_PROCESSING_PROJECT_DIR, Path.class, null, context);
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    public static GeneratedFile visitMetaInfFile(String path, VisitorContext context) {
        var generatedFile = get(MICRONAUT_INTERNAL_GENERATED_FILE + "META-INF/" + path, GeneratedFile.class, null, context);
        if (generatedFile == null) {
            generatedFile = context.visitMetaInfFile(path, Element.EMPTY_ELEMENT_ARRAY).orElse(null);
            if (generatedFile == null || (generatedFile.toURI().getScheme() != null && generatedFile.toURI().getScheme().equals("mem"))) {
                return null;
            }
            put(MICRONAUT_INTERNAL_GENERATED_FILE + "META-INF/" + path, generatedFile, context);
        }
        calcProjectPath(generatedFile, context);
        return generatedFile;
    }

    public static Path getProjectDir(VisitorContext context) {
        Path projectDir = get(MICRONAUT_PROCESSING_PROJECT_DIR, Path.class, context);
        if (projectDir != null) {
            return projectDir;
        }

        var dummyFile = get(MICRONAUT_INTERNAL_GENERATED_FILE + PREFIX_DUMMY_FILE, GeneratedFile.class, null, context);
        if (dummyFile == null) {
            dummyFile = context.visitGeneratedFile(PREFIX_DUMMY_FILE, Element.EMPTY_ELEMENT_ARRAY).orElse(null);
            if (dummyFile == null || (dummyFile.toURI().getScheme() != null && dummyFile.toURI().getScheme().equals("mem"))) {
                return null;
            }
            put(MICRONAUT_INTERNAL_GENERATED_FILE + PREFIX_DUMMY_FILE, dummyFile, context);
        }

        return calcProjectPath(dummyFile, context);
    }

    public static void warn(String message, @Nullable VisitorContext context) {
        warn(message, context, null);
    }

    public static void warn(String message, @Nullable VisitorContext context, @Nullable Element element) {
        if (context != null) {
            context.warn(message, element);
        } else {
            System.err.println(message);
        }
    }

    public static void info(String message, @Nullable VisitorContext context) {
        info(message, context, null);
    }

    public static void info(String message, @Nullable VisitorContext context, @Nullable Element element) {
        if (context != null) {
            context.info(message, element);
        } else {
            System.out.println(message);
        }
    }

    public static void addGeneratedResource(String path, @Nullable VisitorContext context) {
        if (context == null) {
            return;
        }
        context.addGeneratedResource(path);
    }

    public static <T> T get(String paramName, Argument<T> arg, VisitorContext context) {
        return get(paramName, arg, null, context);
    }

    public static <T> T get(String paramName, Argument<T> arg, T defaultValue, VisitorContext context) {
        return context != null ? context.get(paramName, arg).orElse(defaultValue) : defaultValue;
    }

    public static <T> T get(String paramName, Class<T> arg, VisitorContext context) {
        return get(paramName, arg, null, context);
    }

    public static <T> T get(String paramName, Class<T> arg, T defaultValue, VisitorContext context) {
        return context != null ? context.get(paramName, arg).orElse(defaultValue) : defaultValue;
    }

    public static <T> void put(CharSequence paramName, T value, VisitorContext context) {
        if (context != null) {
            context.put(paramName, value);
        }
    }

    public static void remove(String paramName, VisitorContext context) {
        if (context != null) {
            context.remove(paramName);
        }
    }

    public static boolean contains(String paramName, VisitorContext context) {
        if (context != null) {
            return context.contains(paramName);
        }
        return false;
    }

    public static Map<String, String> getOptions(VisitorContext context) {
        if (context != null) {
            return context.getOptions();
        }
        return Collections.emptyMap();
    }

    @Nullable
    public static ClassElement getClassElement(String className, VisitorContext context) {
        return context != null ? context.getClassElement(className).orElse(null) : null;
    }

    @Nullable
    public static ClassElement[] getClassElements(String packageName, VisitorContext context) {
        return context != null ? context.getClassElements(packageName, WILDCARD) : null;
    }
}
