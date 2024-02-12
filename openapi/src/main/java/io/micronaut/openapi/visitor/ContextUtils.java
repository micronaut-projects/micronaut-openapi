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

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.GenericArgument;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.inject.writer.GeneratedFile;
import io.micronaut.openapi.visitor.group.GroupProperties;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_CLASSPATH_OUTPUT;
import static io.micronaut.openapi.visitor.ContextProperty.MICRONAUT_INTERNAL_GENERATED_FILE;

/**
 * Convert utilities methods.
 *
 * @since 4.5.0
 */
@Internal
public final class ContextUtils {

    public static final Argument<List<Tag>> TAGS_LIST_ARGUMENT = new GenericArgument<>() { };
    public static final Argument<List<Server>> SERVERS_LIST_ARGUMENT = new GenericArgument<>() { };
    public static final Argument<List<Pair<String, String>>> EXPANDABLE_PROPERTIES_ARGUMENT = new GenericArgument<>() { };
    public static final Argument<Map<String, ConfigUtils.SchemaDecorator>> ARGUMENT_SCHEMA_DECORATORS_MAP = new GenericArgument<>() { };
    public static final Argument<Map<String, ConfigUtils.CustomSchema>> ARGUMENT_CUSTOM_SCHEMA_MAP = new GenericArgument<>() { };
    public static final Argument<Map<String, GroupProperties>> ARGUMENT_GROUP_PROPERTIES_MAP = new GenericArgument<>() { };

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

    public static Path getClassesOutputPath(VisitorContext context) {

        if (context == null) {
            return null;
        }

        var outputPath = get(MICRONAUT_INTERNAL_CLASSPATH_OUTPUT, Path.class, null, context);
        if (outputPath != null) {
            return outputPath;
        }
        visitMetaInfFile("dummy" + System.nanoTime(), context);
        return get(MICRONAUT_INTERNAL_CLASSPATH_OUTPUT, Path.class, null, context);
    }

    public static GeneratedFile visitMetaInfFile(String path, VisitorContext context) {

        if (context == null) {
            return null;
        }
        var cachedFile = get(MICRONAUT_INTERNAL_GENERATED_FILE + path, GeneratedFile.class, null, context);
        if (cachedFile != null) {
            return cachedFile;
        }
        var generatedFile = context.visitMetaInfFile(path, Element.EMPTY_ELEMENT_ARRAY).orElse(null);
        if (generatedFile == null) {
            warn("Unable to get " + path + " file.", context);
            return null;
        }

        put(MICRONAUT_INTERNAL_GENERATED_FILE + path, generatedFile, context);

        if (!contains(MICRONAUT_INTERNAL_CLASSPATH_OUTPUT, context)) {

            var uri = generatedFile.toURI();
            // happens in tests 'mem:///CLASS_OUTPUT/META-INF/swagger/swagger.yml'
            if (uri.getScheme() != null && !uri.getScheme().equals("mem")) {
                var generatedFilePath = Path.of(uri);
                var count = 0;
                for (var i = 0; i < path.length(); i++) {
                    if (path.charAt(i) == '/') {
                        generatedFilePath = generatedFilePath.getParent();
                        count++;
                    }
                }
                // now this is classesOutputDir, parent of META-INF directory
                generatedFilePath = generatedFilePath.getParent();

                put(MICRONAUT_INTERNAL_CLASSPATH_OUTPUT, generatedFilePath, context);
            }
        }

        return generatedFile;
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

    public static ClassElement getClassElement(String className, VisitorContext context) {
        return context != null ? context.getClassElement(className).orElse(null) : null;
    }
}
