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
package io.micronaut.openapi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.micronaut.context.annotation.AliasFor;
import io.swagger.v3.oas.annotations.extensions.Extension;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * With this annotation, you can specify one or more groups that this endpoint will be included in,
 * as well as specify groups from which this endpoint should be excluded. Also, you can set
 * specific endpoint extensions for each group
 *
 * @since 4.10.0
 */
@Repeatable(OpenAPIGroups.class)
@Retention(SOURCE)
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD})
public @interface OpenAPIGroup {

    /**
     * @return The names of the OpenAPi groups.
     */
    @AliasFor(member = "names")
    String[] value() default {};

    /**
     * @return The names of the OpenAPi groups.
     */
    @AliasFor(member = "value")
    String[] names() default {};

    /**
     * @return The names of the OpenAPi groups to exclude endpoints from.
     */
    String[] exclude() default {};

    /**
     * The list of optional extensions only for these groups.
     *
     * @return an optional array of extensions
     * @since 6.7.0
     */
    Extension[] extensions() default {};
}
