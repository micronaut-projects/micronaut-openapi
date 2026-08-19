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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.MediaType;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.GenericElement;
import io.micronaut.inject.ast.GenericPlaceholderElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
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
     * Generic templates currently being built, keyed by raw type name, mapped to their registered
     * schema name. Used to short-circuit a self-reference inside a template (e.g. {@code Tree<T>}'s
     * {@code List<Tree<T>> children}) to a plain {@code $ref} back to the template, so the
     * recursion re-enters the template in the same dynamic scope and keeps the type-variable
     * binding active at every depth.
     */
    private static Map<String, String> activeTemplates = new HashMap<>();

    /**
     * Named subtypes currently being resolved (keyed by raw type name). Used to detect cycles —
     * direct (WorkspaceFolder extends Folder&lt;WorkspaceFolder&gt;) or mutual (A extends Folder&lt;B&gt;,
     * B extends Folder&lt;A&gt;) — so the second entry returns null and falls back to concrete instead
     * of infinite-looping.
     */
    private static Set<String> resolvingNamedSubtypes = new HashSet<>();

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
        activeTemplates = new HashMap<>();
        resolvingNamedSubtypes = new HashSet<>();
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
     * Returns the anchor bound to the given type-variable name in the generic template currently
     * being built (top of {@link #templateVarStack}), or {@code null} when no template build is
     * active or the variable is not part of it. This is the primary lookup used at field-resolution
     * time: each {@link GenericPlaceholderElement} carries its declared variable name, which selects
     * the correct per-variable anchor even for multi-parameter generics.
     *
     * @param varName the declared type-variable name (e.g. {@code "K"}, {@code "V"}, {@code "T"})
     * @return the matching anchor, or {@code null}
     */
    static String templateAnchorFor(String varName) {
        if (templateVarStack.isEmpty() || varName == null) {
            return null;
        }
        return templateVarStack.get(0).get(varName);
    }

    /**
     * Returns the registered schema name of the generic template currently being built for the
     * given raw type name, or {@code null} if no such template build is active. Used to detect a
     * self-reference inside a template and short-circuit it to a plain {@code $ref}.
     *
     * @param rawTypeName the raw (erased) type name being referenced
     * @return the active template's schema name, or {@code null}
     */
    static String activeTemplateRef(String rawTypeName) {
        return rawTypeName == null ? null : activeTemplates.get(rawTypeName);
    }

    /**
     * Whether the given type is a self-reference to the generic template currently being built that
     * carries only unresolved type-variable arguments (or none) — i.e. the recursive same-variable
     * case such as {@code Tree<T>}'s {@code List<Tree<T>> children}, which must collapse to a plain
     * {@code $ref} back to the template. A self-reference with a concrete argument (e.g.
     * {@code Wrapper<Pet>} inside a {@code Wrapper<T>} template) is <em>not</em> this case: it is a
     * distinct binding and must go through normal resolution so the concrete binding is preserved.
     * <p>
     * Ceiling: a same-template self-reference whose argument is itself a parameterization with
     * unresolved inner variables (e.g. {@code Wrapper<Wrapper<T>>} inside {@code Wrapper<T>}) is not
     * detected as unresolved here (the immediate argument is a concrete {@link ClassElement}), so it
     * goes through normal binding resolution. That exotic shape is out of scope and may emit a
     * recursive rebind that does not preserve the inner variable.
     */
    static boolean isUnresolvedTemplateSelfRef(ClassElement type) {
        Map<String, ClassElement> args = type.getTypeArguments();
        if (args == null || args.isEmpty()) {
            return true;
        }
        for (ClassElement arg : args.values()) {
            if (!(arg instanceof GenericElement) && !(arg instanceof GenericPlaceholderElement)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the single anchor of the template currently being built, when that template has
     * exactly one type variable; {@code null} otherwise. Used for code paths that do not know which
     * variable a usage corresponds to (e.g. a plain {@link GenericElement} with no declared name);
     * for multi-variable templates such usages cannot be disambiguated and fall back to resolution.
     */
    static String currentTemplateAnchor() {
        if (templateVarStack.isEmpty()) {
            return null;
        }
        Map<String, String> frame = templateVarStack.get(0);
        return frame.size() == 1 ? frame.values().iterator().next() : null;
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
     * Derives the {@code $dynamicAnchor} name for every type variable of a generic template, keyed
     * by declared variable name.
     * <p>
     * For a single-variable template the name is <em>role-derived</em> ({@code itemType} when the
     * variable is used as a collection/array element, otherwise {@code dataType}) — this preserves
     * the existing output shape. For multi-variable templates role names would collide (two scalar
     * variables both want {@code dataType}), so each anchor is derived from its declared variable
     * name (e.g. {@code K}, {@code V}), sanitized to a valid JSON Schema anchor and de-duplicated
     * when two names sanitize identically.
     *
     * @param rawType  the erased generic type (whose fields drive the single-variable role)
     * @param typeArgs the type arguments (keyed by declared variable name)
     * @return an ordered map of variable name to anchor name
     */
    static Map<String, String> deriveTemplateAnchors(ClassElement rawType, Map<String, ClassElement> typeArgs) {
        var result = new LinkedHashMap<String, String>();
        if (typeArgs.size() == 1) {
            String varName = typeArgs.keySet().iterator().next();
            result.put(varName, deriveTemplateAnchor(rawType));
            return result;
        }
        var used = new HashSet<String>();
        int suffix = 1;
        for (String varName : typeArgs.keySet()) {
            String base = toDynamicAnchorName(varName);
            String anchor = base;
            while (!used.add(anchor)) {
                anchor = base + "_" + suffix++;
            }
            result.put(varName, anchor);
        }
        return result;
    }

    /**
     * Whether the given parameterized type should be emitted as a dynamic-ref generic binding:
     * dynamic-refs mode is on, it has at least one type argument, it is not a collection / map, and
     * <em>every</em> argument is a concrete reference type.
     * <p>
     * Arguments that are an unresolved type variable, primitive, array, collection or map cannot
     * form a {@code $ref} binding slot; if any argument is such a type, the whole generic keeps its
     * default concrete schema (a partially bound template would have an inconsistent shape).
     * Self-referential generics are supported (the self-reference is resolved as a plain recursive
     * {@code $ref} to the template, see {@link #activeTemplateRef}).
     */
    static boolean isGenericBindingCandidate(ClassElement type, Map<String, ClassElement> typeArgs) {
        if (typeArgs == null || typeArgs.isEmpty()) {
            return false;
        }
        if (ElementUtils.isJavaUtilCollectionType(type) || type.isAssignable(Map.class) || ElementUtils.isContainerType(type)) {
            return false;
        }
        for (ClassElement arg : typeArgs.values()) {
            if (!isReferenceTypeArg(arg)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a single type argument can occupy a {@code $ref} binding slot: a concrete reference
     * type that is not an unresolved variable, primitive, array, collection or map.
     */
    private static boolean isReferenceTypeArg(ClassElement arg) {
        if (arg instanceof GenericElement || arg instanceof GenericPlaceholderElement) {
            return false;
        }
        if (ElementUtils.isJavaBasicType(arg.getName())
            || arg.isArray()
            || ElementUtils.isContainerType(arg)
            || arg.isAssignable(Map.class)) {
            return false;
        }
        return true;
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
     * Reads the {@code $defs} block previously attached by {@link #setSchemaDefs}, or
     * {@code null} if none is present. Used to fold a nested generic binding's rebinding (e.g. the
     * inner {@code Page<Pet>} binding of {@code ApiEnvelope<Page<Pet>>}) into an enclosing slot.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Schema> getSchemaDefs(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        try {
            Map<String, Object> extensions = (Map<String, Object>) SCHEMA_EXTENSIONS_FIELD.get(schema);
            if (extensions == null) {
                return null;
            }
            Object defs = extensions.get("$defs");
            if (defs instanceof Map<?, ?> map) {
                return (Map<String, Schema>) map;
            }
        } catch (IllegalAccessException e) {
            // best-effort
        }
        return null;
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

        Map<String, String> anchors = deriveTemplateAnchors(rawType, typeArgs);
        // A schema object can carry a single $dynamicAnchor; the first variable's anchor is the
        // template's primary anchor (used for the collision guard and the root stamp below). Every
        // variable additionally carries its own anchor via the $defs entries, which is what the
        // $dynamicRef consumers resolve against.
        String primaryAnchor = anchors.values().iterator().next();

        Schema<?> template = schemas.get(templateName);
        if (template != null && !primaryAnchor.equals(template.get$dynamicAnchor())) {
            // A non-template schema already occupies this name (for example a raw usage of the
            // generic). Do not poison it; fall back to the default concrete behavior.
            return null;
        }
        if (template == null) {
            template = SchemaUtils.createSchema();
            template.name(templateName);
            schemas.put(templateName, template);
            // Stamp the primary anchor BEFORE populating properties: a self-referential generic
            // (Tree<T> with List<Tree<T>> children) re-enters itself during population, and the
            // recursion guard in recursiveSchemaRef must see an existing anchor so it falls back
            // to a plain $ref (re-entering the template in the same dynamic scope) instead of
            // stamping a competing recursion anchor on the single $dynamicAnchor slot.
            if (template.get$dynamicAnchor() == null) {
                template.set$dynamicAnchor(primaryAnchor);
            }
            templateVarStack.add(0, new LinkedHashMap<>(anchors));
            activeTemplates.put(rawType.getName(), templateName);
            try {
                SchemaDefinitionUtils.populateSchemaProperties(openApi, context, rawType, rawType.getTypeArguments(), template, mediaTypes, null, jsonViewClass);
            } finally {
                templateVarStack.remove(0);
                activeTemplates.remove(rawType.getName());
            }
            var placeholderDefs = new LinkedHashMap<String, Schema>();
            for (String anchor : anchors.values()) {
                var placeholder = SchemaUtils.createSchema();
                placeholder.set$dynamicAnchor(anchor);
                placeholder.setNot(SchemaUtils.createSchema());
                placeholderDefs.put(anchor, placeholder);
            }
            setSchemaDefs(template, placeholderDefs);
        }

        var slotDefs = new LinkedHashMap<String, Schema>();
        for (var entry : typeArgs.entrySet()) {
            String anchor = anchors.get(entry.getKey());
            ClassElement concreteType = entry.getValue();
            // A type argument that is itself a generic binding candidate (e.g. Page<Pet> as the
            // data type of ApiEnvelope<Page<Pet>>) must be resolved as a binding directly here:
            // getSchemaDefinition's dynamic-refs gate requires a defining element, which the type
            // argument does not have, so delegating there would silently collapse it to concrete.
            // Recursing here builds the inner template and returns its inline binding.
            Schema<?> concreteRef;
            if (isGenericBindingCandidate(concreteType, concreteType.getTypeArguments())) {
                concreteRef = resolveGenericBinding(openApi, context, concreteType, concreteType.getTypeArguments(), mediaTypes, jsonViewClass);
            } else {
                concreteRef = SchemaDefinitionUtils.getSchemaDefinition(openApi, context, concreteType, concreteType.getTypeArguments(), null, mediaTypes, jsonViewClass);
            }
            var slot = SchemaUtils.createSchema();
            slot.set$dynamicAnchor(anchor);
            if (concreteRef != null && concreteRef.get$ref() != null) {
                slot.set$ref(concreteRef.get$ref());
                // Fold the inner binding's inline $defs rebinding into this slot so the nested
                // binding is preserved instead of collapsing to an unbound template reference.
                // Composes recursively for deeper nesting (Outer<Middle<Leaf>>).
                Map<String, Schema> nestedDefs = getSchemaDefs(concreteRef);
                if (nestedDefs != null && !nestedDefs.isEmpty()) {
                    setSchemaDefs(slot, new LinkedHashMap<>(nestedDefs));
                }
            }
            slotDefs.put(anchor, slot);
        }

        var binding = SchemaUtils.createSchema();
        binding.set$ref(SchemaUtils.schemaRef(templateName));
        setSchemaDefs(binding, slotDefs);
        return binding;
    }

    // ---- named subtypes ----------------------------------------------------------------

    /**
     * Whether the given concrete type is a named subtype of a parameterized generic supertype
     * (e.g. {@code class PetResponse extends Response<Pet>}). Self-referential and mutually
     * recursive subtypes are detected at resolution time by {@link #resolveNamedSubtypeBinding}'s
     * in-progress set, not here.
     */
    static boolean isNamedSubtypeBinding(ClassElement type) {
        if (type == null) {
            return false;
        }
        return parameterizedGenericSuperType(type) != null;
    }

    /**
     * Builds a named binding schema for a named subtype of a parameterized generic. A
     * pure-specialization subtype (no fields of its own) becomes a {@code $ref} to the template
     * plus an inline {@code $defs} rebinding. A subtype that adds its own fields becomes an
     * {@code allOf} of that binding and an own-properties object, so the subtype's declared fields
     * are preserved instead of being dropped. The result is meant to be registered under the
     * subtype's own schema name and referenced by {@code $ref}.
     *
     * @return the binding (or composed) schema, or {@code null} if no parameterized generic
     *         supertype is found or its binding cannot be built
     */
    static Schema<?> resolveNamedSubtypeBinding(OpenAPI openApi, VisitorContext context, ClassElement type,
                                                List<MediaType> mediaTypes, @Nullable ClassElement jsonViewClass) {
        String typeName = type.getName();
        if (resolvingNamedSubtypes.contains(typeName)) {
            return null;
        }
        resolvingNamedSubtypes.add(typeName);
        try {
            ClassElement superBinding = parameterizedGenericSuperType(type);
            if (superBinding == null) {
                return null;
            }
            Schema<?> binding = resolveGenericBinding(openApi, context, superBinding, superBinding.getTypeArguments(), mediaTypes, jsonViewClass);
            if (binding == null) {
                return null;
            }
            // Compute both name sets once: superNames is reused for the adds-own-field test and for
            // stripping shadowed inherited names from the own branch below.
            Set<String> superNames = schemaPropertyNames(superBinding);
            Set<String> ownNames = schemaPropertyNames(type);
            ownNames.removeAll(superNames);
            if (ownNames.isEmpty()) {
                // Pure specialization: just the binding alias.
                return binding;
            }
            // The subtype adds its own fields: compose the generic binding with an own-properties
            // object so the extra fields survive (rather than falling back to a fully concrete schema).
            // populateSchemaProperties filters ordinary inherited bean properties/fields by declaring
            // type, but inherited @JsonProperty methods and overridden getters can still reach the
            // own-branch, so strip those against the supertype's emitted keys to avoid duplication.
            Schema<?> own = SchemaUtils.createSchema();
            SchemaDefinitionUtils.populateSchemaProperties(openApi, context, type, type.getTypeArguments(), own, mediaTypes, null, jsonViewClass);
            own.setType("object"); // reassert; swagger-core may drop type on allOf members
            if (own.getProperties() != null && !superNames.isEmpty()) {
                own.getProperties().keySet().removeAll(superNames);
            }
            if (own.getProperties() == null || own.getProperties().isEmpty()) {
                return binding;
            }
            var composed = SchemaUtils.createComposedSchema();
            composed.addAllOfItem(binding);
            composed.addAllOfItem(own);
            return composed;
        } finally {
            resolvingNamedSubtypes.remove(typeName);
        }
    }

    /**
     * Finds the direct supertype (superclass or interface) that is itself a dynamic-ref generic
     * binding candidate, or {@code null}. Deeper inheritance chains are not walked; a subtype that
     * specializes a generic two levels up falls back to the default concrete behavior.
     */
    private static ClassElement parameterizedGenericSuperType(ClassElement type) {
        ClassElement s = type.getSuperType().orElse(null);
        if (s != null && isGenericBindingCandidate(s, s.getTypeArguments())) {
            return s;
        }
        for (ClassElement i : type.getInterfaces()) {
            if (isGenericBindingCandidate(i, i.getTypeArguments())) {
                return i;
            }
        }
        return null;
    }

    private static Set<String> schemaPropertyNames(ClassElement el) {
        Set<String> names = new HashSet<>();
        for (PropertyElement p : el.getBeanProperties()) {
            names.add(p.getName());
        }
        for (FieldElement f : el.getFields()) {
            names.add(f.getName());
        }
        for (MethodElement m : el.getMethods()) {
            if (m.hasAnnotation(JsonProperty.class)) {
                // Use the @JsonProperty value (the emitted key), not the raw method name, so renamed properties match during stripping.
                names.add(m.stringValue(JsonProperty.class).filter(StringUtils::isNotEmpty).orElse(m.getName()));
            }
        }
        return names;
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
