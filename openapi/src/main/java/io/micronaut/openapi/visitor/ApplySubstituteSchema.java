package io.micronaut.openapi.visitor;

import io.micronaut.core.annotation.AnnotationValue;
import io.swagger.v3.oas.models.media.Schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class ApplySubstituteSchema {
    private static final io.swagger.v3.oas.annotations.media.Schema DEFAULT_SCHEMA = DefaultSchema.class.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
    private static final List<ApplyIfNotDefault> APPLY_CHANGE_LIST;

    static {
        APPLY_CHANGE_LIST = new ArrayList<>();
        APPLY_CHANGE_LIST.add(new ApplyIfNotDefault<>("pattern", String.class, DEFAULT_SCHEMA.pattern(), Schema::setPattern));
        APPLY_CHANGE_LIST.add(new ApplyIfNotDefault<>("maximum", String.class, DEFAULT_SCHEMA.maximum(), (s, v) -> s.setMaximum(new BigDecimal(v))));
        APPLY_CHANGE_LIST.add(new ApplyIfNotDefault<>("minimum", String.class, DEFAULT_SCHEMA.minimum(), (s, v) -> s.setMinimum(new BigDecimal(v))));
        APPLY_CHANGE_LIST.add(new ApplyIfNotDefault<>("maxLength", Integer.class, DEFAULT_SCHEMA.maxLength(), Schema::setMaxLength));
        APPLY_CHANGE_LIST.add(new ApplyIfNotDefault<>("minLength", Integer.class, DEFAULT_SCHEMA.minLength(), Schema::setMinLength));
        APPLY_CHANGE_LIST.add(new ApplyIfNotDefault<>("exclusiveMaximum", Boolean.class, DEFAULT_SCHEMA.exclusiveMaximum(), Schema::setExclusiveMaximum));
        APPLY_CHANGE_LIST.add(new ApplyIfNotDefault<>("exclusiveMinimum", Boolean.class, DEFAULT_SCHEMA.exclusiveMinimum(), Schema::setExclusiveMinimum));
        APPLY_CHANGE_LIST.add(new ApplyIfNotDefault<>("example", String.class, DEFAULT_SCHEMA.example(), Schema::setExample));
        APPLY_CHANGE_LIST.add(new ApplyIfNotDefault<>("defaultValue", String.class, DEFAULT_SCHEMA.defaultValue(), Schema::setDefault));
        APPLY_CHANGE_LIST.add(new ApplyIfNotDefault<>("multipleOf", Double.class, DEFAULT_SCHEMA.multipleOf(), (s, v) -> s.setMultipleOf(new BigDecimal(v))));
    }

    /**
     * Used when a substitute schema is used in place of the actual object or schema.  @Schema(implementation=String.class) or @Schema(type="String")
     * Pulls values from the original Schema and applies it to the substitute Schema.
     * @param schemaAnnotation Original Schema
     * @param schema Substitute Schema
     */
    public static void apply(AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnnotation, Schema schema) {
        for (ApplyIfNotDefault apply : APPLY_CHANGE_LIST) {
            apply.apply(schemaAnnotation, schema);
        }
    }

    private static class ApplyIfNotDefault<T> {
        private final String getterName;
        private final Class<T> getterType;
        private final T defaultValue;
        private final BiConsumer<Schema, T> setter;

        public ApplyIfNotDefault(String name, Class<T> typeClass, T defaultValue, BiConsumer<Schema, T> setter) {
            this.getterName = name;
            this.getterType = typeClass;
            this.defaultValue = defaultValue;
            this.setter = setter;
        }

        public void apply(AnnotationValue<io.swagger.v3.oas.annotations.media.Schema> schemaAnnotation , Schema schema) {
            schemaAnnotation.get(getterName, getterType)
                .filter(v -> !Objects.equals(v, defaultValue))
                .ifPresent(v -> setter.accept(schema, v));
        }
    }

    // This is here to give me a place to grab a default Schema object to compare against.
    @io.swagger.v3.oas.annotations.media.Schema
    static class DefaultSchema{
    }
}
