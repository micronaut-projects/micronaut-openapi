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

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.beans.BeanMap;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.EnumElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.OpenApiUtils;
import io.micronaut.openapi.swagger.core.util.PrimitiveType;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.micronaut.openapi.OpenApiUtils.CONVERT_JSON_MAPPER;
import static io.micronaut.openapi.OpenApiUtils.JSON_MAPPER;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.ElementUtils.isEnum;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ALLOWABLE_VALUES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_BEARER_FORMAT;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_CONTENT;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DEFAULT;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DEFAULT_VALUE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_DESCRIPTION;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ENUM;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXAMPLES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_EXTENSIONS;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_FLOWS;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_IN;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_MEDIA_TYPE;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_NAME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_ONE_FORMAT;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_OPEN_ID_CONNECT_URL;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_PARAM_NAME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REF;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_REF_DOLLAR;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SCHEMA;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SCHEME;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_SCOPES;
import static io.micronaut.openapi.visitor.OpenApiModelProp.PROP_TYPE;
import static io.micronaut.openapi.visitor.ProtoUtils.isProtobufGenerated;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_ARRAY;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_BOOLEAN;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_INTEGER;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_NUMBER;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_OBJECT;
import static io.micronaut.openapi.visitor.SchemaUtils.TYPE_STRING;
import static io.micronaut.openapi.visitor.SchemaUtils.processExtensions;
import static io.micronaut.openapi.visitor.StringUtil.COMMA;
import static io.micronaut.openapi.visitor.Utils.resolveComponents;

/**
 * Convert utilities methods.
 *
 * @since 4.4.1
 */
@Internal
public final class ConvertUtils {

    public static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };
    public static final TypeReference<Map<CharSequence, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private ConvertUtils() {
    }

    /**
     * Convert the given Map to a JSON node and then to the specified type.
     *
     * @param <T> The output class type
     * @param values The values
     * @param context The visitor context
     * @param type The class
     *
     * @return The converted instance
     */
    public static <T> T toValue(Map<CharSequence, Object> values, VisitorContext context, Class<T> type) {
        JsonNode node = toJson(values, context);
        try {
            return ConvertUtils.treeToValue(node, type, context);
        } catch (JsonProcessingException e) {
            warn("Error converting  [" + node + "]: to " + type + ":\n" + Utils.printStackTrace(e), context);
        }
        return null;
    }

    /**
     * Convert the given map to a JSON node.
     *
     * @param values The values
     * @param context The visitor context
     *
     * @return The node
     */
    public static JsonNode toJson(Map<CharSequence, Object> values, VisitorContext context) {
        Map<CharSequence, Object> newValues = toValueMap(values, context);
        return JSON_MAPPER.valueToTree(newValues);
    }

    public static Map<CharSequence, Object> toValueMap(Map<CharSequence, Object> values, VisitorContext context) {
        var newValues = new HashMap<CharSequence, Object>(values.size());
        for (Map.Entry<CharSequence, Object> entry : values.entrySet()) {
            CharSequence key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof AnnotationValue<?> av) {
                final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context);
                newValues.put(key, valueMap);
            } else if (value instanceof AnnotationClassValue<?> acv) {
                acv.getType().ifPresent(aClass -> newValues.put(key, aClass));
            } else if (value != null) {
                if (value.getClass().isArray()) {
                    Object[] a = (Object[]) value;
                    if (ArrayUtils.isNotEmpty(a)) {
                        Object first = a[0];
                        boolean areAnnotationValues = first instanceof AnnotationValue;
                        boolean areClassValues = first instanceof AnnotationClassValue;

                        if (areClassValues) {
                            var classes = new ArrayList<Class<?>>(a.length);
                            for (Object o : a) {
                                var acv = (AnnotationClassValue<?>) o;
                                acv.getType().ifPresent(classes::add);
                            }
                            newValues.put(key, classes);
                        } else if (areAnnotationValues) {
                            String annotationName = ((AnnotationValue<?>) first).getAnnotationName();
                            if (io.swagger.v3.oas.annotations.security.SecurityRequirement.class.getName().equals(annotationName)) {
                                var securityRequirements = new ArrayList<SecurityRequirement>(a.length);
                                for (Object o : a) {
                                    securityRequirements.add(ConvertUtils.mapToSecurityRequirement((AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement>) o));
                                }
                                newValues.put(key, securityRequirements);
                            } else if (Extension.class.getName().equals(annotationName)) {
                                var extensions = new HashMap<String, Object>();
                                for (Object o : a) {
                                    processExtensions(extensions, (AnnotationValue<Extension>) o);
                                }
                                newValues.put(PROP_EXTENSIONS, extensions);
                            } else if (Server.class.getName().equals(annotationName)) {
                                var servers = new ArrayList<Map<CharSequence, Object>>();
                                for (Object o : a) {
                                    var sv = (AnnotationValue<ServerVariable>) o;
                                    Map<CharSequence, Object> variables = new LinkedHashMap<>(toValueMap(sv.getValues(), context));
                                    servers.add(variables);
                                }
                                newValues.put(key, servers);
                            } else if (OAuthScope.class.getName().equals(annotationName)) {
                                Map<String, String> params = toTupleSubMap(a, PROP_NAME, PROP_DESCRIPTION);
                                newValues.put(key, params);
                            } else if (ServerVariable.class.getName().equals(annotationName)) {
                                var variables = new LinkedHashMap<String, Map<CharSequence, Object>>();
                                for (Object o : a) {
                                    var sv = (AnnotationValue<ServerVariable>) o;
                                    sv.stringValue(PROP_NAME).ifPresent(name -> {
                                        Map<CharSequence, Object> map = toValueMap(sv.getValues(), context);
                                        Object dv = map.get(PROP_DEFAULT_VALUE);
                                        if (dv != null) {
                                            map.put(PROP_DEFAULT, dv);
                                        }
                                        if (map.containsKey(PROP_ALLOWABLE_VALUES)) {
                                            // The key in the generated openapi needs to be "enum"
                                            map.put(PROP_ENUM, map.remove(PROP_ALLOWABLE_VALUES));
                                        }
                                        variables.put(name, map);
                                    });
                                }
                                newValues.put(key, variables);
                            } else {
                                if (a.length == 1) {
                                    var av = (AnnotationValue<?>) a[0];
                                    final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context);
                                    newValues.put(key, toValueMap(valueMap, context));
                                } else {

                                    var list = new ArrayList<>();
                                    for (Object o : a) {
                                        if (o instanceof AnnotationValue<?> av) {
                                            final Map<CharSequence, Object> valueMap = toValueMap(av.getValues(), context);
                                            list.add(valueMap);
                                        } else {
                                            list.add(o);
                                        }
                                    }
                                    newValues.put(key, list);
                                }
                            }
                        } else {
                            newValues.put(key, value);
                        }
                    } else {
                        newValues.put(key, a);
                    }
                } else {
                    var parsedJsonValue = parseJsonString(value);
                    newValues.put(key, parsedJsonValue != null ? parsedJsonValue : value);
                }
            }
        }
        return newValues;
    }

    public static Map<String, Object> parseJsonString(Object object) {
        if (object instanceof String string) {
            try {
                return CONVERT_JSON_MAPPER.readValue(string, MAP_TYPE_REFERENCE);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Converts Json node into a class' instance or throws 'com.fasterxml.jackson.core.JsonProcessingException', adds extensions if present.
     *
     * @param jn The json node
     * @param clazz The output class instance
     * @param <T> The output class type
     * @param context visitor context
     *
     * @return The converted instance
     *
     * @throws JsonProcessingException if error
     */
    public static <T> T treeToValue(JsonNode jn, Class<T> clazz, VisitorContext context) throws JsonProcessingException {

        var fixed = false;
        T value;
        try {
            value = CONVERT_JSON_MAPPER.treeToValue(jn, clazz);
        } catch (Exception e) {
            // maybe exception with groovy
            if (context.getLanguage() == VisitorContext.Language.GROOVY) {
                value = fixForGroovy(jn, clazz, null);
                fixed = true;
            } else {
                throw e;
            }
        }
        if (!fixed && context.getLanguage() == VisitorContext.Language.GROOVY) {
            value = fixForGroovy(jn, clazz, null);
        }

        if (value == null) {
            return null;
        }

        var finalValue = value;

        var exts = resolveExtensions(jn);
        if (exts != null) {
            BeanMap.of(finalValue).put(PROP_EXTENSIONS, exts);
        }
        String elType = jn.has(PROP_TYPE) ? jn.get(PROP_TYPE).textValue() : null;
        String elFormat = jn.has(PROP_ONE_FORMAT) ? jn.get(PROP_ONE_FORMAT).textValue() : null;
        JsonNode defaultValueNode = jn.get(PROP_DEFAULT_VALUE);
        // fix for default value
        Object defaultValue;
        try {
            defaultValue = ConvertUtils.normalizeValue(defaultValueNode != null ? defaultValueNode.textValue() : null, elType, elFormat, context);
        } catch (JsonProcessingException e) {
            defaultValue = defaultValueNode != null ? defaultValueNode.textValue() : null;
        }

        BeanMap<T> beanMap = BeanMap.of(value);
        if (defaultValue != null) {
            beanMap.put(PROP_DEFAULT, defaultValue);
        }

        JsonNode allowableValuesNode = jn.get(PROP_ALLOWABLE_VALUES);
        if (allowableValuesNode != null && allowableValuesNode.isArray()) {
            var allowableValues = new ArrayList<>(allowableValuesNode.size());
            for (JsonNode allowableValueNode : allowableValuesNode) {
                if (allowableValueNode == null) {
                    continue;
                }
                try {
                    allowableValues.add(ConvertUtils.normalizeValue(allowableValueNode.textValue(), elType, elFormat, context));
                } catch (IOException e) {
                    allowableValues.add(allowableValueNode.textValue());
                }
            }
            beanMap.put(PROP_ALLOWABLE_VALUES, allowableValues);
        }

        return value;
    }

    private static <T> Map<String, T> deserMap(String name, JsonNode jn, Class<T> clazz) throws JsonProcessingException {

        var mapNode = jn.get(name);
        if (mapNode == null) {
            return null;
        }
        ((ObjectNode) jn).remove(name);

        var iter = mapNode.fieldNames();
        var result = new HashMap<String, T>();
        while (iter.hasNext()) {
            var entryKey = iter.next();
            var objectNode = mapNode.get(entryKey);
            var object = CONVERT_JSON_MAPPER.treeToValue(objectNode, clazz);
            result.put(entryKey, object);
        }
        return !result.isEmpty() ? result : null;
    }

    private static <T> T fixForGroovy(JsonNode jn, Class<T> clazz, Exception e) throws JsonProcessingException {

        // fix for problem with groovy. Jackson throw exception with Operation class with content and mediaType
        // see https://github.com/micronaut-projects/micronaut-openapi/issues/1418
        if (clazz == Operation.class) {

            var requestBodyNode = jn.get("requestBody");
            ((ObjectNode) jn).remove("requestBody");
            T value = CONVERT_JSON_MAPPER.treeToValue(jn, clazz);
            var requestBody = fixContentForGroovy(requestBodyNode, RequestBody.class);
            ((Operation) value).setRequestBody(requestBody);

            var responsesNode = jn.get("responses");
            ((ObjectNode) jn).remove("responses");
            ApiResponses responses = null;
            if (responsesNode != null && !responsesNode.isEmpty()) {
                responses = new ApiResponses();
                var iter = responsesNode.fields();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    responses.put(entry.getKey(), fixContentForGroovy(entry.getValue(), ApiResponse.class));
                }
            }
            ((Operation) value).setResponses(responses);
            return value;
        } else if (clazz == ApiResponse.class
            || clazz == Header.class
            || clazz == Parameter.class
            || clazz == RequestBody.class) {
            return fixContentForGroovy(jn, clazz);
        } else {
            return CONVERT_JSON_MAPPER.treeToValue(jn, clazz);
        }
    }

    private static <T> T fixContentForGroovy(JsonNode parentNode, Class<T> clazz) throws JsonProcessingException {
        if (parentNode == null) {
            return null;
        }
        Map<String, Example> examples = null;
        Map<String, Encoding> encoding = null;
        Map<String, Object> extensions = null;
        Schema<?> schema = null;
        JsonNode mediaTypeNode = null;

        var contentNode = parentNode.get(PROP_CONTENT);
        if (contentNode != null) {
            examples = deserMap(PROP_EXAMPLES, contentNode, Example.class);
            encoding = deserMap("encoding", contentNode, Encoding.class);
            extensions = deserMap(PROP_EXTENSIONS, contentNode, Object.class);
            var schemaNode = contentNode.get(PROP_SCHEMA);
            if (schemaNode != null) {
                schema = CONVERT_JSON_MAPPER.treeToValue(schemaNode, Schema.class);
            }

            mediaTypeNode = contentNode.get(PROP_MEDIA_TYPE);
            ((ObjectNode) contentNode).remove(PROP_MEDIA_TYPE);
        }
        var value = CONVERT_JSON_MAPPER.treeToValue(parentNode, clazz);
        Content content = null;
        if (value instanceof ApiResponse apiResponse) {
            content = apiResponse.getContent();
        } else if (value instanceof Header header) {
            content = header.getContent();
        } else if (value instanceof Parameter parameter) {
            content = parameter.getContent();
        } else if (value instanceof RequestBody requestBody) {
            content = requestBody.getContent();
        }

        if (content != null) {
            var mediaType = content.get(PROP_SCHEMA);
            content.remove(PROP_SCHEMA);
            if (mediaType == null) {
                mediaType = new MediaType();
            }
            var contentType = mediaTypeNode != null ? mediaTypeNode.textValue() : io.micronaut.http.MediaType.APPLICATION_JSON;
            content.put(contentType, mediaType);
            mediaType.setExamples(examples);
            mediaType.setEncoding(encoding);
            if (extensions != null) {
                extensions.forEach(mediaType::addExtension);
            }
            mediaType.setSchema(schema);
        }

        return value;
    }

    private static void processMediaType(Content result, JsonNode content) throws JsonProcessingException {
        var mediaType = content.has(PROP_MEDIA_TYPE) ? content.get(PROP_MEDIA_TYPE).asText() : io.micronaut.http.MediaType.APPLICATION_JSON;
        var mediaTypeObj = CONVERT_JSON_MAPPER.treeToValue(content, MediaType.class);
        result.addMediaType(mediaType, mediaTypeObj);
    }

    public static Object normalizeValue(String valueStr, String type, String format, VisitorContext context) throws JsonProcessingException {
        return normalizeValue(valueStr, type, format, context, false);
    }

    public static Object normalizeValue(String valueStr, String type, String format, VisitorContext context, boolean isMicronautFormat) throws JsonProcessingException {
        if (valueStr == null) {
            return null;
        }
        if (type == null || type.equals(TYPE_OBJECT)) {
            return CONVERT_JSON_MAPPER.readValue(valueStr, Map.class);
        }
        return parseByTypeAndFormat(valueStr, type, format, context, isMicronautFormat);
    }

    public static Map<String, Object> resolveExtensions(JsonNode jn) {
        try {
            JsonNode extensionsNode = jn.get(PROP_EXTENSIONS);
            if (extensionsNode != null) {
                return CONVERT_JSON_MAPPER.convertValue(extensionsNode, MAP_TYPE_REFERENCE);
            }
        } catch (IllegalArgumentException e) {
            // Ignore
        }
        return null;
    }

    public static void addSecuritySchemes(OpenAPI openApi,
                                          List<AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityScheme>> values,
                                          VisitorContext context) {
        for (var securityRequirementAnnValue : values) {

            final Map<CharSequence, Object> map = toValueMap(securityRequirementAnnValue.getValues(), context);

            var name = securityRequirementAnnValue.stringValue(PROP_NAME).orElse(null);
            if (StringUtils.isEmpty(name)) {
                continue;
            }
            if (map.containsKey(PROP_PARAM_NAME)) {
                map.put(PROP_NAME, map.remove(PROP_PARAM_NAME));
            }

            Utils.normalizeEnumValues(map, CollectionUtils.mapOf(PROP_TYPE, SecurityScheme.Type.class, PROP_IN, SecurityScheme.In.class));

            var type = (String) map.get(PROP_TYPE);
            if (!SecurityScheme.Type.APIKEY.toString().equals(type)) {
                removeAndWarnSecSchemeProp(map, PROP_NAME, context, false);
                removeAndWarnSecSchemeProp(map, PROP_IN, context);
            }
            if (!SecurityScheme.Type.OAUTH2.toString().equals(type)) {
                removeAndWarnSecSchemeProp(map, PROP_FLOWS, context);
            }
            if (!SecurityScheme.Type.OPENIDCONNECT.toString().equals(type)) {
                removeAndWarnSecSchemeProp(map, PROP_OPEN_ID_CONNECT_URL, context);
            }
            if (!SecurityScheme.Type.HTTP.toString().equals(type)) {
                removeAndWarnSecSchemeProp(map, PROP_SCHEME, context);
                removeAndWarnSecSchemeProp(map, PROP_BEARER_FORMAT, context);
            }

            if (SecurityScheme.Type.HTTP.toString().equals(type)) {
                if (!map.containsKey(PROP_SCHEME)) {
                    warn("Can't use http security scheme without 'scheme' property", context);
                } else if (!map.get(PROP_SCHEME).equals("bearer") && map.containsKey(PROP_BEARER_FORMAT)) {
                    warn("Should NOT have a `bearerFormat` property without `scheme: bearer` being set", context);
                }
            }

            if (map.containsKey(PROP_REF) || map.containsKey(PROP_REF_DOLLAR)) {
                Object ref = map.get(PROP_REF);
                if (ref == null) {
                    ref = map.get(PROP_REF_DOLLAR);
                }
                map.clear();
                map.put(PROP_REF_DOLLAR, ref);
            }

            try {
                JsonNode node = toJson(map, context);
                SecurityScheme securityScheme = ConvertUtils.treeToValue(node, SecurityScheme.class, context);
                if (securityScheme != null) {
                    var exts = resolveExtensions(node);
                    if (exts != null) {
                        BeanMap.of(securityScheme).put(PROP_EXTENSIONS, exts);
                    }
                    resolveComponents(openApi).addSecuritySchemes(name, securityScheme);
                }
            } catch (JsonProcessingException e) {
                // ignore
            }
        }
    }

    private static void removeAndWarnSecSchemeProp(Map<CharSequence, Object> map, String prop, VisitorContext context) {
        removeAndWarnSecSchemeProp(map, prop, context, true);
    }

    private static void removeAndWarnSecSchemeProp(Map<CharSequence, Object> map, String prop, VisitorContext context, boolean withWarn) {
        if (map.containsKey(prop) && withWarn) {
            warn("'" + prop + "' property can't set for securityScheme with type " + map.get(PROP_TYPE) + ". Skip it", context);
        }
        map.remove(prop);
    }

    /**
     * Maps annotation value to {@link io.swagger.v3.oas.annotations.security.SecurityRequirement}.
     * Correct format is:
     * custom_name:
     * - custom_scope1
     * - custom_scope2
     *
     * @param r The value of {@link SecurityRequirement}.
     *
     * @return converted object.
     */
    public static SecurityRequirement mapToSecurityRequirement(AnnotationValue<io.swagger.v3.oas.annotations.security.SecurityRequirement> r) {
        String name = r.getRequiredValue(PROP_NAME, String.class);
        var scopes = Arrays.asList(r.stringValues(PROP_SCOPES));
        var securityRequirement = new SecurityRequirement();
        securityRequirement.addList(name, scopes);
        return securityRequirement;
    }

    public static void setDefaultValueObject(Schema<?> schema, String defaultValue, @Nullable Element element, @Nullable String schemaType, @Nullable String schemaFormat, boolean isMicronautFormat, VisitorContext context) {
        try {
            Pair<String, String> typeAndFormat;
            if (element instanceof EnumElement enumEl && isEnum(enumEl)) {
                typeAndFormat = ConvertUtils.checkEnumJsonValueType(context, enumEl, schemaType, schemaFormat);
            } else {
                typeAndFormat = Pair.of(schemaType, schemaFormat);
            }
            schema.setDefault(ConvertUtils.normalizeValue(defaultValue, typeAndFormat.getFirst(), typeAndFormat.getSecond(), context, isMicronautFormat));
        } catch (JsonProcessingException e) {
            warn("Can't convert " + defaultValue + " to " + schemaType + ": " + e.getMessage(), context);
            schema.setDefault(defaultValue);
        }
    }

    /**
     * Detect openapi type and format for enums.
     *
     * @param context visitor context
     * @param type enum element
     * @param schemaType type from swagger Schema annotation
     * @param schemaFormat format from swagger Schema annotation
     *
     * @return pair with openapi type and format
     */
    @NonNull
    public static Pair<String, String> checkEnumJsonValueType(VisitorContext context, @NonNull EnumElement type, @Nullable String schemaType, @Nullable String schemaFormat) {
        if (schemaType != null && !schemaType.equals(PrimitiveType.STRING.getCommonName())) {
            return Pair.of(schemaType, schemaFormat);
        }
        MethodElement firstMethod = null;
        // check JsonValue method
        List<MethodElement> methods = type.getEnclosedElements(ElementQuery.ALL_METHODS.annotated(metadata -> metadata.isAnnotationPresent(JsonValue.class)));
        if (CollectionUtils.isNotEmpty(methods)) {
            firstMethod = methods.get(0);
            if (methods.size() > 1) {
                warn("Found " + methods.size() + " methods with @JsonValue. Process method " + firstMethod, context, type);
            }
        } else {
            // Check interfaces annotations
            for (var interfaceEl : type.getInterfaces()) {
                methods = interfaceEl.getEnclosedElements(ElementQuery.ALL_METHODS.annotated(metadata -> metadata.isAnnotationPresent(JsonValue.class)));
                if (methods.isEmpty()) {
                    continue;
                }
                firstMethod = methods.get(0);
                if (methods.size() > 1) {
                    warn("Found " + methods.size() + " methods with @JsonValue. Process method " + firstMethod, context, type);
                }
                break;
            }
        }

        Pair<String, String> result = null;
        if (firstMethod != null) {
            ClassElement returnType = firstMethod.getReturnType();
            if (isEnum(returnType)) {
                return checkEnumJsonValueType(context, (EnumElement) returnType, null, null);
            }
            result = ConvertUtils.getTypeAndFormatByClass(returnType.getName(), returnType.isArray(), returnType);
        }

        if (result == null) {
            // check JsonValue field
            var fields = type.getEnclosedElements(ElementQuery.ALL_FIELDS.annotated(metadata -> metadata.isAnnotationPresent(JsonValue.class)));
            if (CollectionUtils.isNotEmpty(fields)) {
                var firstField = fields.get(0);
                ClassElement fieldType = firstField.getType();
                if (isEnum(fieldType)) {
                    return checkEnumJsonValueType(context, (EnumElement) fieldType, null, null);
                }
                result = ConvertUtils.getTypeAndFormatByClass(fieldType.getName(), fieldType.isArray(), fieldType);
            }
        }
        if (result == null && isProtobufGenerated(type)) {
            return Pair.of(PrimitiveType.INT.getCommonName(), schemaFormat);
        }
        return result != null ? result : Pair.of(PrimitiveType.STRING.getCommonName(), schemaFormat);
    }

    /**
     * Detect openapi type and format by java class name.
     *
     * @param className java class name
     * @param isArray is it array
     * @param classEl class element
     *
     * @return pair with openapi type and format
     */
    public static Pair<String, String> getTypeAndFormatByClass(String className, boolean isArray, @Nullable ClassElement classEl) {
        if (className == null) {
            return Pair.of(TYPE_OBJECT, null);
        }

        if (String.class.getName().equals(className)
            || char.class.getName().equals(className)
            || Character.class.getName().equals(className)) {
            return Pair.of(TYPE_STRING, null);
        } else if (Boolean.class.getName().equals(className)
            || boolean.class.getName().equals(className)) {
            return Pair.of(TYPE_BOOLEAN, null);
        } else if (Integer.class.getName().equals(className)
            || int.class.getName().equals(className)
            || Short.class.getName().equals(className)
            || short.class.getName().equals(className)) {
            return Pair.of(TYPE_INTEGER, "int32");
        } else if (BigInteger.class.getName().equals(className)) {
            return Pair.of(TYPE_INTEGER, null);
        } else if (Long.class.getName().equals(className)
            || long.class.getName().equals(className)) {
            return Pair.of(TYPE_INTEGER, "int64");
        } else if (Float.class.getName().equals(className)
            || float.class.getName().equals(className)) {
            return Pair.of(TYPE_NUMBER, "float");
        } else if (Double.class.getName().equals(className)
            || double.class.getName().equals(className)) {
            return Pair.of(TYPE_NUMBER, "double");
        } else if (isArray && (Byte.class.getName().equals(className)
            || byte.class.getName().equals(className))) {
            return Pair.of(TYPE_STRING, "byte");
            // swagger doesn't support type byte
        } else if (Byte.class.getName().equals(className)
            || byte.class.getName().equals(className)) {
            return Pair.of(TYPE_INTEGER, "int32");
        } else if (BigDecimal.class.getName().equals(className)) {
            return Pair.of(TYPE_NUMBER, null);
        } else if (URI.class.getName().equals(className)) {
            return Pair.of(TYPE_STRING, "uri");
        } else if (URL.class.getName().equals(className)) {
            return Pair.of(TYPE_STRING, "url");
        } else if (UUID.class.getName().equals(className)) {
            return Pair.of(TYPE_STRING, "uuid");
        } else if (Number.class.getName().equals(className)) {
            return Pair.of(TYPE_NUMBER, null);
        } else if (File.class.getName().equals(className)) {
            return Pair.of(TYPE_STRING, "binary");
        } else if (LocalDate.class.getName().equals(className)) {
            return Pair.of(TYPE_STRING, "date");
        } else if (Date.class.getName().equals(className)
            || Calendar.class.getName().equals(className)
            || Instant.class.getName().equals(className)
            || LocalDateTime.class.getName().equals(className)
            || OffsetDateTime.class.getName().equals(className)
            || XMLGregorianCalendar.class.getName().equals(className)
            || ZonedDateTime.class.getName().equals(className)
        ) {
            return Pair.of(TYPE_STRING, "date-time");
        } else if (LocalTime.class.getName().equals(className)) {
            return Pair.of(TYPE_STRING, "partial-time");
        } else {
            if (classEl != null && ElementUtils.isContainerType(classEl)) {
                var typeArg = classEl.getFirstTypeArgument().orElse(null);
                if (typeArg != null) {
                    return getTypeAndFormatByClass(typeArg.getName(), typeArg.isArray(), typeArg);
                }
            }
            return Pair.of(TYPE_OBJECT, null);
        }
    }

    /**
     * Parse value by openapi type and format.
     *
     * @param valueStr string value for parse
     * @param type openapi type
     * @param format openapi value
     * @param context visitor context
     * @param isMicronautFormat is it micronaut format for arrays
     *
     * @return parsed value
     */
    public static Object parseByTypeAndFormat(String valueStr, String type, String format, VisitorContext context, boolean isMicronautFormat) {
        if (valueStr == null) {
            return null;
        }

        // @QueryValue(defaultValue = "")
        if (TYPE_ARRAY.equals(type) && isMicronautFormat) {
            return valueStr.split(COMMA);
        }

        if (valueStr.isEmpty()) {
            return null;
        }

        try {
            if (TYPE_STRING.equals(type)) {
                if ("uri".equals(format)) {
                    return new URI(valueStr);
                } else if ("url".equals(format)) {
                    return new URL(valueStr);
                } else if ("uuid".equals(format)) {
                    return UUID.fromString(valueStr);
                } else if (format == null) {
                    return valueStr;
                }
            } else if (TYPE_BOOLEAN.equals(type)) {
                return Boolean.parseBoolean(valueStr);
            } else if (TYPE_ARRAY.equals(type)) {
                return JSON_MAPPER.readValue(valueStr, List.class);
            } else if (TYPE_INTEGER.equals(type)) {
                if ("int32".equals(format)) {
                    return Integer.parseInt(valueStr);
                } else if ("int64".equals(format)) {
                    return Long.parseLong(valueStr);
                } else {
                    return new BigInteger(valueStr);
                }
            } else if (TYPE_NUMBER.equals(type)) {
                if ("float".equals(format)) {
                    return Float.parseFloat(valueStr);
                } else if ("double".equals(format)) {
                    return Double.parseDouble(valueStr);
                } else {
                    return new BigDecimal(valueStr);
                }
            } else if (TYPE_OBJECT.equals(type) || type == null) {
                try {
                    return OpenApiUtils.getConvertJsonMapper().readValue(valueStr, Map.class);
                } catch (Exception e) {
                    // do nothing
                }
            }
        } catch (Exception e) {
            warn("Can't parse value " + valueStr + " with type " + type + " and format " + format, context);
        }

        return valueStr;
    }

    public static Map<String, String> toTupleSubMap(Object[] a, String entryKey, String entryValue) {
        var params = new LinkedHashMap<String, String>();
        for (Object o : a) {
            var sv = (AnnotationValue<?>) o;
            final Optional<String> n = sv.stringValue(entryKey);
            final Optional<String> expr = sv.stringValue(entryValue);
            if (n.isPresent() && expr.isPresent()) {
                params.put(n.get(), expr.get());
            }
        }
        return params;
    }
}
