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
package io.micronaut.openapi.mapper;

import io.micronaut.core.annotation.Experimental;

/**
 * The OpenAPI specification version a mapper reads and writes.
 *
 * @since 8.0.0
 */
@Experimental
public enum OpenApiVersion {

    /**
     * OpenAPI 3.0.
     */
    V3_0,

    /**
     * OpenAPI 3.1.
     */
    V3_1;

    /**
     * @param openapi31 Whether the document is OpenAPI 3.1
     * @return The matching version
     */
    public static OpenApiVersion of(boolean openapi31) {
        return openapi31 ? V3_1 : V3_0;
    }
}
