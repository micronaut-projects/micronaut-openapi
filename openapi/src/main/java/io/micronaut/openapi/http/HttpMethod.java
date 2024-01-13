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
package io.micronaut.openapi.http;

import io.micronaut.core.annotation.Internal;

/**
 * An enum containing the valid HTTP methods. Simplified copy of HttpMethod class from http module.
 *
 * @since 6.5.0
 */
@Internal
public enum HttpMethod {

    OPTIONS,
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    TRACE,
    CONNECT,
    PATCH,
    CUSTOM;

    /**
     * Whether the given method requires a request body.
     *
     * @param method The HttpMethod
     *
     * @return True if it does
     */
    public static boolean requiresRequestBody(HttpMethod method) {
        return method == POST || method == PUT || method == PATCH;
    }

    /**
     * Whether the given method allows a request body.
     *
     * @param method The HttpMethod
     *
     * @return True if it does
     */
    public static boolean permitsRequestBody(HttpMethod method) {
        return method != null && (requiresRequestBody(method)
            || method == OPTIONS
            || method == DELETE
            || method == CUSTOM
        );
    }
}
