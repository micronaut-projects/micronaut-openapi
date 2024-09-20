/*
 * Copyright 2017-2024 original authors
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
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.openapi.swagger.core.util.PrimitiveType;
import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayList;
import java.util.List;

import static io.micronaut.openapi.visitor.SchemaUtils.setNullable;
import static io.micronaut.openapi.visitor.SchemaUtils.setSpecVersion;

/**
 * Protobuf util methods.
 *
 * @since 6.8.0
 */
@Internal
public final class ProtoUtils {

    public static List<PropertyElement> filterProtobufProperties(ClassElement classElement, List<PropertyElement> beanProperties) {
        if (!isProtobufGenerated(classElement)) {
            return beanProperties;
        }
        var propertiesWithoutProto = new ArrayList<PropertyElement>();
        for (var prop : beanProperties) {
            if (prop.getName().equals("initialized")
                || prop.getName().equals("defaultInstanceForType")
                || prop.getName().equals("initializationErrorString")
                || prop.getName().equals("descriptorForType")
                || prop.getName().equals("allFields")
                || prop.getName().equals("unknownFields")
            ) {
                continue;
            }
            var readMethod = prop.getReadMethod().orElse(null);
            if (readMethod != null) {

                var returnType = readMethod.getReturnType();

                if (readMethod.getName().endsWith("OrBuilderList")
                    || (readMethod.getName().endsWith("Bytes")
                    && returnType.getName().equals("com.google.protobuf.ByteString"))
                ) {
                    continue;
                }

                // process map properties
                if (returnType.getName().equals("java.util.Map")
                    && readMethod.getName().endsWith("Map")) {
                    continue;
                }

                // for enum fields need to skip getValue methods
                if (readMethod.getName().endsWith("Value")) {
                    var enumMethod = classElement.findMethod(readMethod.getName().substring(0, readMethod.getName().lastIndexOf("Value"))).orElse(null);
                    if (enumMethod != null && enumMethod.getReturnType().isEnum()) {
                        continue;
                    }
                }

                // for iterable fields need to skip getCount methods
                if (readMethod.getName().endsWith("Count")
                    && classElement.findMethod(readMethod.getName().substring(0, readMethod.getName().lastIndexOf("Count"))).isPresent()) {
                    continue;
                }

                // skip protobuf internal properties for objects
                if (isProtobufGenerated(returnType)
                    && prop.getName().endsWith("OrBuilder")) {
                    var propName = prop.getName().substring(0, prop.getName().lastIndexOf("OrBuilder"));
                    var found = false;
                    for (var propWithoutSuffix : beanProperties) {
                        if (propName.equals(propWithoutSuffix.getName())) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        continue;
                    }
                }
            }
            propertiesWithoutProto.add(prop);
        }
        return propertiesWithoutProto;
    }

    @SuppressWarnings("MissingSwitchDefault")
    public static Schema protobufTypeSchema(ClassElement type) {

        if (!isProtobufType(type)) {
            return null;
        }

        var typeName = type.getName();
        switch (typeName) {
            case "com.google.protobuf.ByteString" -> {
                return setSpecVersion(PrimitiveType.BYTE.createProperty());
            }
            // wrapper types
            case "com.google.protobuf.BytesValueOrBuilder" -> {
                return setNullable(setSpecVersion(PrimitiveType.BYTE.createProperty()));
            }
            case "com.google.protobuf.DoubleValueOrBuilder" -> {
                return setNullable(setSpecVersion(PrimitiveType.DOUBLE.createProperty()));
            }
            case "com.google.protobuf.FloatValueOrBuilder" -> {
                return setNullable(setSpecVersion(PrimitiveType.FLOAT.createProperty()));
            }
            case "com.google.protobuf.BoolValueOrBuilder" -> {
                return setNullable(setSpecVersion(PrimitiveType.BOOLEAN.createProperty()));
            }
            case "com.google.protobuf.StringValueOrBuilder" -> {
                return setNullable(setSpecVersion(PrimitiveType.STRING.createProperty()));
            }
            case "com.google.protobuf.Int32ValueOrBuilder",
                 "com.google.protobuf.UInt32ValueOrBuilder" -> {
                return setNullable(setSpecVersion(PrimitiveType.INT.createProperty()));
            }
            case "com.google.protobuf.Int64ValueOrBuilder",
                 "com.google.protobuf.UInt64ValueOrBuilder" -> {
                return setSpecVersion(PrimitiveType.LONG.createProperty());
            }
            case "com.google.protobuf.AnyOrBuilder",
                 "com.google.protobuf.ApiOrBuilder",
                 "com.google.protobuf.DurationOrBuilder",
                 "com.google.protobuf.EmptyOrBuilder" -> {
                return null;
            }
        }
        return null;
    }

    public static String normalizePropertyName(String propertyName, ClassElement classElement, ClassElement propertyType) {
        if (!isProtobufGenerated(classElement) || !propertyType.isIterable()) {
            return propertyName;
        }
        var listIndex = propertyName.lastIndexOf("List");
        if (listIndex > 0) {
            propertyName = propertyName.substring(0, listIndex);
        }
        return propertyName;
    }

    public static String normalizeProtobufClassName(String className) {
        var endIndex = className.indexOf("OrBuilder");
        if (endIndex < 0) {
            return className;
        }
        return className.substring(0, endIndex);
    }

    public static boolean isProtobufType(ClassElement type) {
        return type != null && type.getPackageName().startsWith("com.google.protobuf");
    }

    public static boolean isProtobufGenerated(ClassElement type) {
        return type != null && (
            type.isAssignable("com.google.protobuf.MessageOrBuilder")
                || type.isAssignable("com.google.protobuf.ProtocolMessageEnum")
        );
    }

    public static boolean isProtobufMessageClass(ClassElement type) {
        return type != null && (
            type.isAssignable("com.google.protobuf.GeneratedMessageV3")
                || type.isAssignable("com.google.protobuf.GeneratedMessage")
                || type.isAssignable("com.google.protobuf.GeneratedMessageLite")
        );
    }
}
