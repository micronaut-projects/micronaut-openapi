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
package io.micronaut.openapi.swagger.core.util;

import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;

import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.entry;

/**
 * The {@code PrimitiveType} enumeration defines a mapping of limited set
 * of classes into Swagger primitive types.
 * <p>
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public enum PrimitiveType {
    STRING(String.class, "string") {
        @Override
        public Schema<?> createProperty() {
            return new StringSchema();
        }
    },
    BOOLEAN(Boolean.class, "boolean") {
        @Override
        public Schema<?> createProperty() {
            return new BooleanSchema();
        }
    },
    BYTE(Byte.class, "byte") {
        @Override
        public Schema<?> createProperty() {
            if (
                (System.getProperty(Schema.BINARY_STRING_CONVERSION_PROPERTY) != null && System.getProperty(Schema.BINARY_STRING_CONVERSION_PROPERTY).equals(Schema.BynaryStringConversion.BINARY_STRING_CONVERSION_STRING_SCHEMA.toString())) ||
                    (System.getenv(Schema.BINARY_STRING_CONVERSION_PROPERTY) != null && System.getenv(Schema.BINARY_STRING_CONVERSION_PROPERTY).equals(Schema.BynaryStringConversion.BINARY_STRING_CONVERSION_STRING_SCHEMA.toString()))) {
                return new StringSchema().format("byte");
            }
            return new ByteArraySchema();
        }
    },
    BINARY(Byte.class, "binary") {
        @Override
        public Schema<?> createProperty() {
            if (
                (System.getProperty(Schema.BINARY_STRING_CONVERSION_PROPERTY) != null && System.getProperty(Schema.BINARY_STRING_CONVERSION_PROPERTY).equals(Schema.BynaryStringConversion.BINARY_STRING_CONVERSION_STRING_SCHEMA.toString())) ||
                    (System.getenv(Schema.BINARY_STRING_CONVERSION_PROPERTY) != null && System.getenv(Schema.BINARY_STRING_CONVERSION_PROPERTY).equals(Schema.BynaryStringConversion.BINARY_STRING_CONVERSION_STRING_SCHEMA.toString()))) {
                return new StringSchema().format("binary");
            }
            return new BinarySchema();
        }
    },
    URI(java.net.URI.class, "uri") {
        @Override
        public Schema<?> createProperty() {
            return new StringSchema().format("uri");
        }
    },
    URL(java.net.URL.class, "url") {
        @Override
        public Schema<?> createProperty() {
            return new StringSchema().format("url");
        }
    },
    EMAIL(String.class, "email") {
        @Override
        public Schema<?> createProperty() {
            return new StringSchema().format("email");
        }
    },
    UUID(java.util.UUID.class, "uuid") {
        @Override
        public UUIDSchema createProperty() {
            return new UUIDSchema();
        }
    },
    INT(Integer.class, "integer") {
        @Override
        public IntegerSchema createProperty() {
            return new IntegerSchema();
        }
    },
    LONG(Long.class, "long") {
        @Override
        public Schema<?> createProperty() {
            return new IntegerSchema().format("int64");
        }
    },
    FLOAT(Float.class, "float") {
        @Override
        public Schema<?> createProperty() {
            return new NumberSchema().format("float");
        }
    },
    DOUBLE(Double.class, "double") {
        @Override
        public Schema<?> createProperty() {
            return new NumberSchema().format("double");
        }
    },
    INTEGER(BigInteger.class) {
        @Override
        public Schema<?> createProperty() {
            return new IntegerSchema().format(null);
        }
    },
    DECIMAL(BigDecimal.class, "number") {
        @Override
        public Schema<?> createProperty() {
            return new NumberSchema();
        }
    },
    NUMBER(Number.class, "number") {
        @Override
        public Schema<?> createProperty() {
            return new NumberSchema();
        }
    },
    DATE(DateStub.class, "date") {
        @Override
        public DateSchema createProperty() {
            return new DateSchema();
        }
    },
    DATE_TIME(Date.class, "date-time") {
        @Override
        public DateTimeSchema createProperty() {
            return new DateTimeSchema();
        }
    },
    PARTIAL_TIME(LocalTime.class, "partial-time") {
        @Override
        public Schema<?> createProperty() {
            return new StringSchema().format("partial-time");
        }
    },
    FILE(File.class, "file") {
        @Override
        public FileSchema createProperty() {
            return new FileSchema();
        }
    },
    OBJECT(Object.class) {
        @Override
        public Schema<?> createProperty() {
            return new Schema<>().type("object");
        }
    };

    private static final Map<Class<?>, PrimitiveType> KEY_CLASSES;
    private static final Map<Class<?>, Collection<PrimitiveType>> MULTI_KEY_CLASSES;
    private static final Map<Class<?>, PrimitiveType> BASE_CLASSES;
    /**
     * Adds support of a small number of "well-known" types, specifically for
     * Joda lib.
     */
    private static final Map<String, PrimitiveType> EXTERNAL_CLASSES;

    /**
     * Allows to exclude specific classes from KEY_CLASSES mappings to primitive
     */
    private static final Set<String> customExcludedClasses = ConcurrentHashMap.newKeySet();

    /**
     * Allows to exclude specific classes from EXTERNAL_CLASSES mappings to primitive
     */
    private static final Set<String> customExcludedExternalClasses = ConcurrentHashMap.newKeySet();

    /**
     * Adds support for custom mapping of classes to primitive types
     */
    private static final Map<String, PrimitiveType> customClasses = new ConcurrentHashMap<>();

    /**
     * class qualified names prefixes to be considered as "system" types
     */
    private static final Set<String> systemPrefixes = ConcurrentHashMap.newKeySet();
    /**
     * class qualified names NOT to be considered as "system" types
     */
    private static final Set<String> nonSystemTypes = ConcurrentHashMap.newKeySet();
    /**
     * package names NOT to be considered as "system" types
     */
    private static final Set<String> nonSystemTypePackages = ConcurrentHashMap.newKeySet();

    /**
     * Alternative names for primitive types that have to be supported for
     * backward compatibility.
     */
    private static final Map<String, PrimitiveType> NAMES;
    private final Class<?> keyClass;
    private final String commonName;

    public static final Map<String, String> datatypeMappings;

    static {
        systemPrefixes.add("java.");
        systemPrefixes.add("javax.");

        datatypeMappings = Map.ofEntries(
            entry("integer_int32", "integer"),
            entry("integer_", "integer"),
            entry("integer_int64", "long"),
            entry("number_", "number"),
            entry("number_float", "float"),
            entry("number_double", "double"),
            entry("string_", "string"),
            entry("string_byte", "byte"),
            entry("string_email", "email"),
            entry("string_binary", "binary"),
            entry("string_uri", "uri"),
            entry("string_url", "url"),
            entry("string_uuid", "uuid"),
            entry("string_date", "date"),
            entry("string_date-time", "date-time"),
            entry("string_partial-time", "partial-time"),
            entry("string_password", "password"),
            entry("boolean_", "boolean"),
            entry("object_", "object")
        );

        final var keyClasses = new HashMap<Class<?>, PrimitiveType>();
        addKeys(keyClasses, BOOLEAN, Boolean.class, Boolean.TYPE);
        addKeys(keyClasses, STRING, String.class, Character.class, CharSequence.class, Character.TYPE);
        addKeys(keyClasses, BYTE, Byte.class, Byte.TYPE);
        addKeys(keyClasses, URL, java.net.URL.class);
        addKeys(keyClasses, URI, java.net.URI.class);
        addKeys(keyClasses, UUID, java.util.UUID.class);
        addKeys(keyClasses, INT, Integer.class, Integer.TYPE, Short.class, Short.TYPE);
        addKeys(keyClasses, LONG, Long.class, Long.TYPE);
        addKeys(keyClasses, FLOAT, Float.class, Float.TYPE);
        addKeys(keyClasses, DOUBLE, Double.class, Double.TYPE);
        addKeys(keyClasses, INTEGER, BigInteger.class);
        addKeys(keyClasses, DECIMAL, BigDecimal.class);
        addKeys(keyClasses, NUMBER, Number.class);
        addKeys(keyClasses, DATE, DateStub.class);
        addKeys(keyClasses, DATE_TIME, Date.class);
        addKeys(keyClasses, FILE, File.class);
        addKeys(keyClasses, OBJECT, Object.class);

        // OpenAPI specification haven't 'format' for these 'java.time' classes and other 'java.util' classes
        addKeys(keyClasses, STRING,
            Duration.class,
            Period.class,
            LocalTime.class,
            OffsetTime.class,
            YearMonth.class,
            Year.class,
            MonthDay.class,
            ZoneId.class,
            ZoneOffset.class,
            TimeZone.class,
            Charset.class,
            Locale.class
        );

        KEY_CLASSES = Collections.unmodifiableMap(keyClasses);

        final var multiKeyClasses = new HashMap<Class<?>, Collection<PrimitiveType>>();
        addMultiKeys(multiKeyClasses, BYTE, byte[].class);
        addMultiKeys(multiKeyClasses, BINARY, byte[].class);
        MULTI_KEY_CLASSES = Collections.unmodifiableMap(multiKeyClasses);

        final var baseClasses = new HashMap<Class<?>, PrimitiveType>();
        addKeys(baseClasses, DATE_TIME, Date.class, Calendar.class);
        BASE_CLASSES = Collections.unmodifiableMap(baseClasses);

        final var externalClasses = new HashMap<String, PrimitiveType>();
        addKeys(externalClasses, DATE, "org.joda.time.LocalDate", LocalDate.class.getName());
        addKeys(externalClasses, DATE_TIME,
            LocalDateTime.class.getName(),
            ZonedDateTime.class.getName(),
            OffsetDateTime.class.getName(),
            Instant.class.getName(),
            "javax.xml.datatype.XMLGregorianCalendar",
            "org.joda.time.LocalDateTime",
            "org.joda.time.ReadableDateTime",
            "org.joda.time.DateTime");
        EXTERNAL_CLASSES = Collections.unmodifiableMap(externalClasses);

        var names = new TreeMap<String, PrimitiveType>(String.CASE_INSENSITIVE_ORDER);
        for (PrimitiveType item : values()) {
            final String name = item.commonName;
            if (name != null) {
                addKeys(names, item, name);
            }
        }
        addKeys(names, INT, "int");
        addKeys(names, OBJECT, "object");
        NAMES = Collections.unmodifiableMap(names);
    }

    PrimitiveType(Class<?> keyClass) {
        this(keyClass, null);
    }

    PrimitiveType(Class<?> keyClass, String commonName) {
        this.keyClass = keyClass;
        this.commonName = commonName;
    }

    /**
     * Adds support for custom mapping of classes to primitive types
     *
     * @return Set of custom classes to primitive type
     * @since 2.0.6
     */
    public static Set<String> customExcludedClasses() {
        return customExcludedClasses;
    }

    /**
     * Adds support for custom mapping of classes to primitive types
     *
     * @return Set of custom classes to primitive type
     * @since 2.1.2
     */
    public static Set<String> customExcludedExternalClasses() {
        return customExcludedExternalClasses;
    }

    /**
     * Adds support for custom mapping of classes to primitive types
     *
     * @return Map of custom classes to primitive type
     * @since 2.0.6
     */
    public static Map<String, PrimitiveType> customClasses() {
        return customClasses;
    }

    /**
     * class qualified names prefixes to be considered as "system" types
     *
     * @return Mutable set of class qualified names prefixes to be considered as "system" types
     * @since 2.0.6
     */
    public static Set<String> systemPrefixes() {
        return systemPrefixes;
    }

    /**
     * class qualified names NOT to be considered as "system" types
     *
     * @return Mutable set of class qualified names NOT to be considered as "system" types
     * @since 2.0.6
     */
    public static Set<String> nonSystemTypes() {
        return nonSystemTypes;
    }

    /**
     * package names NOT to be considered as "system" types
     *
     * @return Mutable set of package names NOT to be considered as "system" types
     * @since 2.0.6
     */
    public static Set<String> nonSystemTypePackages() {
        return nonSystemTypePackages;
    }

    public static PrimitiveType fromTypeAndFormat(Type type, String format) {
        final Class<?> raw = TypeFactory.defaultInstance().constructType(type).getRawClass();
        final Collection<PrimitiveType> keys = MULTI_KEY_CLASSES.get(raw);
        if (keys == null || keys.isEmpty() || format == null || format.isBlank()) {
            return fromType(type);
        } else {
            return keys
                .stream()
                .filter(t -> t.getCommonName().equalsIgnoreCase(format))
                .findAny()
                .orElse(null);
        }
    }

    public static PrimitiveType fromType(Type type) {
        final Class<?> raw = TypeFactory.defaultInstance().constructType(type).getRawClass();
        final PrimitiveType key = KEY_CLASSES.get(raw);
        if (key != null && !customExcludedClasses.contains(raw.getName())) {
            return key;
        }

        final Collection<PrimitiveType> keys = MULTI_KEY_CLASSES.get(raw);
        if (keys != null && !keys.isEmpty()) {
            final PrimitiveType first = keys.iterator().next();
            if (!customExcludedClasses.contains(raw.getName())) {
                return first;
            }
        }

        final PrimitiveType custom = customClasses.get(raw.getName());
        if (custom != null) {
            return custom;
        }

        final PrimitiveType external = EXTERNAL_CLASSES.get(raw.getName());
        if (external != null && !customExcludedExternalClasses().contains(raw.getName())) {
            return external;
        }

        for (Map.Entry<Class<?>, PrimitiveType> entry : BASE_CLASSES.entrySet()) {
            if (entry.getKey().isAssignableFrom(raw)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static PrimitiveType fromName(String name) {
        if (name == null) {
            return null;
        }
        PrimitiveType fromName = NAMES.get(name);
        if (fromName == null && !customExcludedExternalClasses().contains(name)) {
            fromName = EXTERNAL_CLASSES.get(name);
        }
        return fromName;
    }

    public static PrimitiveType fromTypeAndFormat(String type, String format) {
        if ("object".equals(type)) {
            return null;
        }
        return fromName(datatypeMappings.get(String.format("%s_%s", type != null && !type.isBlank() ? type : "", format != null && !format.isBlank() ? format : "")));
    }

    public static Schema<?> createProperty(Type type) {
        final PrimitiveType item = fromType(type);
        return item == null ? null : item.createProperty();
    }

    public static Schema<?> createProperty(String name) {
        final PrimitiveType item = fromName(name);
        return item == null ? null : item.createProperty();
    }

    public static String getCommonName(Type type) {
        final PrimitiveType item = fromType(type);
        return item == null ? null : item.commonName;
    }

    public Class<?> getKeyClass() {
        return keyClass;
    }

    public String getCommonName() {
        return commonName;
    }

    public abstract Schema<?> createProperty();

    @SafeVarargs
    private static <K> void addKeys(Map<K, PrimitiveType> map, PrimitiveType type, K... keys) {
        for (K key : keys) {
            map.put(key, type);
        }
    }

    @SafeVarargs
    private static <K> void addMultiKeys(Map<K, Collection<PrimitiveType>> map, PrimitiveType type, K... keys) {
        for (K key : keys) {
            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<>());
            }
            map.get(key).add(type);
        }
    }

    private static class DateStub {

    }

    /**
     * Convenience method to map LocalTime to string primitive with rfc3339 format partial-time.
     * See <a href="https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html#anchor14">link</a>
     *
     * @since 2.0.6
     */
    public static void enablePartialTime() {
        customClasses().put("org.joda.time.LocalTime", PrimitiveType.PARTIAL_TIME);
        customClasses().put("java.time.LocalTime", PrimitiveType.PARTIAL_TIME);
    }
}
