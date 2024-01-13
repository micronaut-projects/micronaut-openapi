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
package io.micronaut.context.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Copy of AliasFor annotation from inject module.
 *
 * @since 6.5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Repeatable(Aliases.class)
public @interface AliasFor {

    /**
     * @return The name of the member that {@code this} member is an alias for
     */
    String member() default "";

    /**
     * @return The type of annotation in which the aliased {@link #member()} is declared.
     *     If not specified the alias is applied to the current annotation.
     */
    Class<? extends Annotation> annotation() default Annotation.class;

    /**
     * @return The name of the annotation in which the aliased {@link #member()} is declared.
     *     If not specified the alias is applied to the current annotation.
     */
    String annotationName() default "";
}
