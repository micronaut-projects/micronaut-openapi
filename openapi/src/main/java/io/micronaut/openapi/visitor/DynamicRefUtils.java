/*
 * Copyright 2017-2026 original authors
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
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.MediaType;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.GenericElement;
import io.micronaut.inject.ast.GenericPlaceholderElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helpers for the opt-in dynamic-refs mode ({@code micronaut.openapi.schema.dynamic-refs.enabled},
 * OpenAPI 3.1 only): emission of JSON Schema 2020-12 {@code $dynamicAnchor} / {@code $dynamicRef}
 * from recursive types and single-variable generic templates.
 *
 * <p>Extracted from {@link SchemaDefinitionUtils} to keep that class focused on schema parsing.
 *
 * @since 7.0.1
 */
@Internal
public final class DynamicRefUtils {

    /**
     * Stores schema names detected as recursive (a property references the enclosing type),
     * mapped to the valid {@code $dynamicAnchor} name generated for the schema.
     * Used to stamp a {@code $dynamicAnchor} on the registered schema and emit
     * {@code $dynamicRef} consumers in dynamic-refs mode.
     */
    private static Map<String, String> recursiveSchemaAnchors = new HashMap<>();

    /**
     * Stack of type-variable frames (var name -&gt; anchor) for generic templates currently
     * being built in dynamic-refs mode. While non-empty, an unresolved type variable is emitted
     * as a {@code $dynamicRef} consumer instead of being resolved to its bound.
     */
    private static List<Map<String, String>> templateVarStack = new ArrayList<>();

    /**
     * Reflective handle to {@link Schema}'s private {@code extensions} map, used by
     * {@link #setSchemaDefs} to inject a {@code $defs} block (a JSON Schema 2020-12 keyword that
     * swagger-core 2.2.x does not model). See {@link #setSchemaDefs} for the full rationale.
     */
    private static final java.lang.reflect.Field SCHEMA_EXTENSIONS_FIELD;

    static {
        try {
            SCHEMA_EXTENSIONS_FIELD = Schema.class.getDeclaredField("extensions");
            SCHEMA_EXTENSIONS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError("io.swagger.v3.oas.models.media.Schema.extensions field not found: " + e.getMessage());
        }
    }

    private DynamicRefUtils() {
    }

    /**
     * Cleanup context.
     */
    public static void clean() {
        recursiveSchemaAnchors = new HashMap<>();
        templateVarStack = new ArrayList<>();
    }

    // ---- recursive types ----------------------------------------------------------------

    /**
     * Returns a schema reference for a type that recurses into itself while still being processed.
     * <p>
     * In dynamic-refs mode (OpenAPI 3.1), emits a {@code $dynamicRef} and ensures a matching
     * {@code $dynamicAnchor} is stamped on the registered schema, so that JSON Schema 2020-12
     * dynamic-scope resolution can switch the reference to an active subtype (e.g. resolving
     * {@code children} to {@code LocalizedCategory} instead of {@code BaseCategory}). Outside
     * dynamic-refs mode, falls back to a normal {@code $ref}, preserving the default behavior.
     *
     * @param schemaName the name of the recursive schema
     * @param schemas    the components schemas map
     * @param context    the visitor context
     * @return a {@code $dynamicRef} (dynamic-refs mode) or a {@code $ref} (default)
     */
    static Schema<?> recursiveSchemaRef(String schemaName, Map<String, Schema> schemas, VisitorContext context) {
        if (Utils.isOpenapi31() && ConfigUtils.isDynamicRefsEnabled(context)) {
            String dynamicAnchor = toDynamicAnchorName(schemaName);
            Schema<?> registered = schemas.get(schemaName);
            if (registered != null && registered.get$dynamicAnchor() != null && !dynamicAnchor.equals(registered.get$dynamicAnchor())) {
                return SchemaUtils.createSchema()
                    .$ref(SchemaUtils.schemaRef(schemaName));
            }
            recursiveSchemaAnchors.put(schemaName, dynamicAnchor);
            stampRecursiveAnchor(schemaName, schemas);
            Schema<?> dynamicRef = SchemaUtils.createSchema();
            dynamicRef.set$dynamicRef("#" + dynamicAnchor);
            return dynamicRef;
        }
        return SchemaUtils.createSchema()
            .$ref(SchemaUtils.schemaRef(schemaName));
    }

    /**
     * Stamps the {@code $dynamicAnchor} (equal to the schema name) on the registered schema
     * when recursion into it has been detected. Idempotent: only sets the anchor if missing.
     *
     * @param schemaName the name of the recursive schema
     * @param schemas    the components schemas map
     */
    private static void stampRecursiveAnchor(String schemaName, Map<String, Schema> schemas) {
        Schema<?> registered = schemas.get(schemaName);
        String dynamicAnchor = recursiveSchemaAnchors.get(schemaName);
        if (registered != null && dynamicAnchor != null && registered.get$dynamicAnchor() == null) {
            registered.set$dynamicAnchor(dynamicAnchor);
        }
    }

    /**
     * Stamps the recursive {@code $dynamicAnchor} on a schema at registration time, if recursion
     * into it has been detected. No-op otherwise.
     *
     * @param schema     the schema being registered
     * @param schemaName the schema name
     */
    static void stampAnchorIfRecursive(Schema<?> schema, String schemaName) {
        String dynamicAnchor = recursiveSchemaAnchors.get(schemaName);
        if (schema != null && dynamicAnchor != null && schema.get$dynamicAnchor() == null) {
            schema.set$dynamicAnchor(dynamicAnchor);
        }
    }

    /**
     * Returns the anchor for the single type variable of the generic template currently being built
     * (top of {@link #templateVarStack}), or {@code null} when no template build is active.
     */
    static String currentTemplateAnchor() {
        return templateVarStack.isEmpty() ? null : templateVarStack.get(0).values().iterator().next();
    }

    // ---- generic templates --------------------------------------------------------------

    /**
     * Derives the {@code $dynamicAnchor} name for a generic template from how its single type
     * variable is used: {@code itemType} when the variable is used as a collection/array element,
     * otherwise {@code dataType}.
     */
    static String deriveTemplateAnchor(ClassElement rawType) {
        for (FieldElement field : rawType.getFields()) {
            ClassElement ft = field.getGenericType();
            if (ft == null) {
                continue;
            }
            if (ft.isIterable() || ft.isArray()) {
                ClassElement element = ft.getFirstTypeArgument().orElse(null);
                if (element instanceof GenericElement || element instanceof GenericPlaceholderElement) {
                    return "itemType";
                }
            }
        }
        return "dataType";
    }

    /**
     * Whether the given parameterized type should be emitted as a dynamic-ref generic binding:
     * dynamic-refs mode is on, it has exactly one type argument, it is not a collection / map,
     * and the argument is a concrete reference type (not an unresolved type variable, primitive,
     * array, collection or map — those cannot form a {@code $ref} binding slot).
     */
    static boolean isGenericBindingCandidate(ClassElement type, Map<String, ClassElement> typeArgs) {
        if (typeArgs == null || typeArgs.size() != 1) {
            return false;
        }
        if (ElementUtils.isJavaUtilCollectionType(type) || type.isAssignable(Map.class) || ElementUtils.isContainerType(type)) {
            return false;
        }
        ClassElement arg = typeArgs.values().iterator().next();
        if (arg instanceof GenericElement || arg instanceof GenericPlaceholderElement) {
            return false;
        }
        if (ElementUtils.isJavaBasicType(arg.getName())
            || arg.isArray()
            || ElementUtils.isContainerType(arg)
            || arg.isAssignable(Map.class)) {
            return false;
        }
        // Self-referential generics combine the template and recursion mechanisms in ways that
        // produce inconsistent output today (the recursive descent leaks the template context).
        // Keep the default concrete behavior for them until that interaction is resolved.
        if (isRecursiveType(type)) {
            return false;
        }
        return true;
    }

    /**
     * Whether the given type references itself in one of its fields (directly or through type
     * arguments), i.e. it is self-referential / recursive.
     */
    private static boolean isRecursiveType(ClassElement type) {
        if (type == null) {
            return false;
        }
        String name = type.getName();
        for (FieldElement field : type.getFields()) {
            if (referencesType(field.getGenericType(), name, new HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    private static boolean referencesType(ClassElement el, String targetName, Set<String> seen) {
        if (el == null || !seen.add(el.getName())) {
            return false;
        }
        if (targetName.equals(el.getName())) {
            return true;
        }
        if (referencesType(el.getFirstTypeArgument().orElse(null), targetName, seen)) {
            return true;
        }
        for (ClassElement arg : el.getTypeArguments().values()) {
            if (referencesType(arg, targetName, seen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets a {@code $defs} block on a schema.
     * <p>
     * <b>Workaround:</b> swagger-core 2.2.x {@link Schema} has no {@code $defs} field, and
     * {@link Schema#addExtension(String, Object)} rejects keys that do not start with {@code x-}.
     * The serializer, however, emits every entry of the {@code extensions} map verbatim via
     * {@code @JsonAnyGetter}, so {@code $defs} is injected directly into that map by reflection.
     * When a future swagger-core exposes a native {@code set$defs}, the reflection branch below
     * auto-switches to it; remove this workaround once the minimum supported swagger-core ships it.
     *
     * @param schema the schema to attach {@code $defs} to
     * @param defs   the {@code $defs} entries (anchor name -&gt; schema)
     */
    private static void setSchemaDefs(Schema<?> schema, Map<String, Schema> defs) {
        if (schema == null || defs == null || defs.isEmpty()) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> extensions = (Map<String, Object>) SCHEMA_EXTENSIONS_FIELD.get(schema);
            if (extensions == null) {
                extensions = new LinkedHashMap<>();
                SCHEMA_EXTENSIONS_FIELD.set(schema, extensions);
            }
            extensions.put("$defs", defs);
        } catch (IllegalAccessException e) {
            // best-effort: if reflection is blocked, $defs is silently omitted
        }
    }

    /**
     * Builds a dynamic-ref generic binding for a parameterized type such as {@code Response<Pet>}.
     * <p>
     * Emits one reusable template schema (e.g. {@code Response}) whose type-variable usages become
     * {@code $dynamicRef} consumers and which carries a {@code $defs} placeholder
     * ({@code $dynamicAnchor} + {@code not: {}}), and returns an inline binding at the usage site
     * that re-declares the anchor via {@code $defs} pointing at the concrete argument. The wrapper
     * instance validates against the template only; the concrete schema applies solely where the
     * {@code $dynamicRef} sits.
     *
     * @param openApi       the OpenAPI
     * @param context       the visitor context
     * @param type          the parameterized generic type (e.g. {@code Response<Pet>})
     * @param typeArgs      the concrete type arguments (e.g. {@code {T: Pet}})
     * @param mediaTypes    the media types
     * @param jsonViewClass the JSON view class
     * @return an inline {@code $defs} binding schema, or {@code null} if the template cannot be built
     */
    static Schema<?> resolveGenericBinding(OpenAPI openApi, VisitorContext context, ClassElement type,
                                           Map<String, ClassElement> typeArgs,
                                           List<MediaType> mediaTypes, @Nullable ClassElement jsonViewClass) {
        var schemas = SchemaUtils.resolveSchemas(openApi);
        ClassElement rawType = context.getClassElement(type.getName()).orElse(null);
        if (rawType == null) {
            return null;
        }
        String templateName = SchemaDefinitionUtils.computeDefaultSchemaName(
            SchemaDefinitionUtils.getNameFromAnn(rawType), rawType, rawType, Collections.emptyMap(), context, jsonViewClass);

        var varEntry = typeArgs.entrySet().iterator().next();
        String varName = varEntry.getKey();
        String anchor = deriveTemplateAnchor(rawType);

        Schema<?> template = schemas.get(templateName);
        if (template != null && !anchor.equals(template.get$dynamicAnchor())) {
            // A non-template schema already occupies this name (for example a raw usage of the
            // generic). Do not poison it; fall back to the default concrete behavior.
            return null;
        }
        if (template == null) {
            template = SchemaUtils.createSchema();
            template.name(templateName);
            schemas.put(templateName, template);
            var frame = new LinkedHashMap<String, String>();
            frame.put(varName, anchor);
            templateVarStack.add(0, frame);
            try {
                SchemaDefinitionUtils.populateSchemaProperties(openApi, context, rawType, rawType.getTypeArguments(), template, mediaTypes, null, jsonViewClass);
            } finally {
                templateVarStack.remove(0);
            }
            if (template.get$dynamicAnchor() == null) {
                template.set$dynamicAnchor(anchor);
            }
            var placeholder = SchemaUtils.createSchema();
            placeholder.set$dynamicAnchor(anchor);
            placeholder.setNot(SchemaUtils.createSchema());
            var placeholderDefs = new LinkedHashMap<String, Schema>();
            placeholderDefs.put(anchor, placeholder);
            setSchemaDefs(template, placeholderDefs);
        }

        ClassElement concreteType = varEntry.getValue();
        Schema<?> concreteRef = SchemaDefinitionUtils.getSchemaDefinition(openApi, context, concreteType, concreteType.getTypeArguments(), null, mediaTypes, jsonViewClass);

        var slot = SchemaUtils.createSchema();
        slot.set$dynamicAnchor(anchor);
        if (concreteRef != null && concreteRef.get$ref() != null) {
            slot.set$ref(concreteRef.get$ref());
        }
        var slotDefs = new LinkedHashMap<String, Schema>();
        slotDefs.put(anchor, slot);

        var binding = SchemaUtils.createSchema();
        binding.set$ref(SchemaUtils.schemaRef(templateName));
        setSchemaDefs(binding, slotDefs);
        return binding;
    }

    // ---- anchor name sanitization -------------------------------------------------------

    static String toDynamicAnchorName(String schemaName) {
        if (StringUtils.isEmpty(schemaName)) {
            return "_";
        }
        var result = new StringBuilder(schemaName.length() + 1);
        for (int i = 0; i < schemaName.length(); i++) {
            char c = schemaName.charAt(i);
            if (i == 0 && !isDynamicAnchorStart(c)) {
                result.append('_');
            }
            result.append(isDynamicAnchorPart(c) ? c : '_');
        }
        return result.toString();
    }

    private static boolean isDynamicAnchorStart(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
    }

    private static boolean isDynamicAnchorPart(char c) {
        return isDynamicAnchorStart(c) || (c >= '0' && c <= '9') || c == '-' || c == '.';
    }
}
