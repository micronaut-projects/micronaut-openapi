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
package io.micronaut.openapi.visitor.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.TypeConverter;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpMethod;

/**
 * Copy of class io.micronaut.security.config.InterceptUrlMapConverter from micronaut-security.
 *
 * @since 4.8.7
 */
@Internal
public class InterceptUrlMapConverter implements TypeConverter<Map, InterceptUrlMapPattern> {

    private static final String PATTERN = "pattern";
    private static final String ACCESS = "access";
    private static final String HTTP_METHOD = "http-method";

    private final ConversionService conversionService;

    /**
     * @param conversionService The conversion service
     */
    public InterceptUrlMapConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * @param m a Map in the configuration
     * @param targetType The target type being converted to
     * @param context The {@link ConversionContext}
     *
     * @return An optional InterceptUrlMapConverter
     */
    @Override
    public Optional<InterceptUrlMapPattern> convert(Map m, Class<InterceptUrlMapPattern> targetType, ConversionContext context) {
        if (m == null) {
            return Optional.empty();
        }
        m = transform(m);
        Optional<String> optionalPattern = conversionService.convert(m.get(PATTERN), String.class);
        if (optionalPattern.isPresent()) {
            Optional<List<String>> optionalAccessList = conversionService.convert(m.get(ACCESS), Argument.LIST_OF_STRING);
            optionalAccessList = optionalAccessList.map(list -> list.stream()
                .map(o -> conversionService.convert(o, String.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(String.class::cast)
                .collect(Collectors.toList()));
            if (optionalAccessList.isPresent()) {
                Optional<HttpMethod> httpMethod;
                if (m.containsKey(HTTP_METHOD)) {
                    httpMethod = conversionService.convert(m.get(HTTP_METHOD), HttpMethod.class);
                    if (httpMethod.isEmpty()) {
                        throw new ConfigurationException(String.format("interceptUrlMap configuration record %s rejected due to invalid %s key.", m, HTTP_METHOD));
                    }
                } else {
                    httpMethod = Optional.empty();
                }
                List<String> accessList = optionalAccessList.get();
                return optionalPattern
                    .map(pattern -> new InterceptUrlMapPattern(pattern, accessList, httpMethod.orElse(null)));
            } else {
                throw new ConfigurationException(String.format("interceptUrlMap configuration record %s rejected due to missing or empty %s key.", m, ACCESS));
            }
        } else {
            throw new ConfigurationException(String.format("interceptUrlMap configuration record %s rejected due to missing %s key.", m, PATTERN));
        }
    }

    private Map<String, Object> transform(Map<String, Object> map) {
        Map<String, Object> transformed = new HashMap<>();
        StringConvention convention = StringConvention.HYPHENATED;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            transformed.put(convention.format(entry.getKey()), entry.getValue());
        }
        return transformed;
    }
}
