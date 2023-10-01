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
package io.micronaut.openapi.adoc.utils;

import java.util.Collection;
import java.util.Map;

/**
 * CollectionUtils.
 *
 * @since 5.2.0
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T, R> boolean isNotEmpty(Map<T, R> map) {
        return !isEmpty(map);
    }

    public static <T, R> boolean isEmpty(Map<T, R> map) {
        return map == null || map.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }
}
