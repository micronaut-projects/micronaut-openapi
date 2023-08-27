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
package io.micronaut.openapi;

import java.util.Objects;

import io.swagger.v3.oas.models.media.Schema;

/**
 * Copy of MapSchema but without type 'object'. Need this class to correct deserializing schema without type.
 *
 * @since 4.8.7
 */
public class SimpleSchema extends Schema<Object> {

    public SimpleSchema() {
        super(null, null);
    }

    @Override
    public SimpleSchema type(String type) {
        super.setType(type);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @Override
    public String toString() {
        return "class SimpleSchema {\n" +
            "    " + toIndentedString(super.toString()) + "\n" +
            "}";
    }
}
