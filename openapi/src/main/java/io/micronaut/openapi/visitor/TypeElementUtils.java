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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.inject.ast.TypedElement;

/**
 * Some util methods.
 *
 * @since 4.8.3
 */
public final class TypeElementUtils {

    private TypeElementUtils() {
    }

    /**
     * Checks Nullable annotions / optinal type to understand that the element can be null.
     *
     * @param element typed element
     *
     * @return true if element is nullable, false - otherwise.
     */
    public static boolean isNullable(TypedElement element) {
        return element.isNullable()
            || element.getType().isOptional()
            || element.hasStereotype(Nullable.class)
            || element.hasStereotype("javax.annotation.Nullable")
            || element.hasStereotype("jakarta.annotation.Nullable")
            || element.hasStereotype("org.jetbrains.annotations.Nullable")
            || element.hasStereotype("androidx.annotation.Nullable")
            || element.hasStereotype("edu.umd.cs.findbugs.annotations.Nullable")
            || element.hasStereotype("org.eclipse.jdt.annotation.Nullable")
            || element.hasStereotype("io.reactivex.annotations.Nullable")
            || element.hasStereotype("io.reactivex.rxjava3.annotations.Nullable")
            || element.hasStereotype("reactor.util.annotation.Nullable")
            || element.hasStereotype("org.jspecify.annotations.Nullable");
    }
}
