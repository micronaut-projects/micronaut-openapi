/*
 * Copyright 2017-2022 original authors
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

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.server.types.files.FileCustomizableResponseType;
import io.micronaut.inject.ast.ClassElement;
import io.swagger.v3.oas.models.security.SecurityRequirement;

import org.reactivestreams.Publisher;

/**
 * Some util methods.
 *
 * @since 4.4.0
 */
public final class Utils {

    private Utils() {
    }

    public static boolean isContainerType(ClassElement type) {
        return CollectionUtils.setOf(
            Optional.class.getName(),
            Future.class.getName(),
            Publisher.class.getName(),
            "io.reactivex.Single",
            "io.reactivex.Observable",
            "io.reactivex.Maybe",
            "io.reactivex.rxjava3.core.Single",
            "io.reactivex.rxjava3.core.Observable",
            "io.reactivex.rxjava3.core.Maybe"
        ).stream().anyMatch(type::isAssignable);
    }

    public static boolean isReturnTypeFile(ClassElement type) {
        return CollectionUtils.setOf(
            FileCustomizableResponseType.class.getName(),
            File.class.getName(),
            InputStream.class.getName(),
            ByteBuffer.class.getName()
        ).stream().anyMatch(type::isAssignable);
    }

    /**
     * Normalizes enum values stored in the map.
     *
     * @param paramValues The values
     * @param enumTypes The enum types.
     */
    public static void normalizeEnumValues(Map<CharSequence, Object> paramValues, Map<String, Class<? extends Enum>> enumTypes) {
        for (Map.Entry<String, Class<? extends Enum>> entry : enumTypes.entrySet()) {
            final String name = entry.getKey();
            final Class<? extends Enum> enumType = entry.getValue();
            Object in = paramValues.get(name);
            if (in != null) {
                try {
                    final Enum enumInstance = Enum.valueOf(enumType, in.toString());
                    paramValues.put(name, enumInstance.toString());
                } catch (Exception e) {
                    // ignore
                }
            }

        }
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
        String name = r.getRequiredValue("name", String.class);
        List<String> scopes = r.get("scopes", String[].class).map(Arrays::asList).orElse(Collections.emptyList());
        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.addList(name, scopes);
        return securityRequirement;
    }
}
