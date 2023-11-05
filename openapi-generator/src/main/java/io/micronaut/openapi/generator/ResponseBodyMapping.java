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
package io.micronaut.openapi.generator;

import java.util.Objects;

/**
 * A record that can be used to specify parameter mapping.
 * Parameter mapping would map a given parameter to a specific type and name.
 *
 * @param headerName The response header name that triggers the change of response type.
 * @param mappedBodyType The type in which will be used as the response type. The type must take
 *                    a single type parameter, which will be the original body.
 * @param isListWrapper Whether the mapped body type needs to be supplied list items
 *                      as property.
 * @param isValidated Whether the mapped response body type required validation.
 */
public record ResponseBodyMapping(
    String headerName,
    String mappedBodyType,
    boolean isListWrapper,
    boolean isValidated
) {

    public boolean doesMatch(String header, boolean isBodyList) {
        if (isListWrapper && !isBodyList) {
            return false;
        }
        return Objects.equals(headerName, header);
    }
}
