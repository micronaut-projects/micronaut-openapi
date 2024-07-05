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

import io.micronaut.context.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * With this annotation, you can specify one or more groups that this endpoint will be included in,
 * as well as specify groups from which this endpoint should be excluded. Also, you can set
 * specific endpoint extensions for each group
 *
 * @since 6.12.0
 */
@Repeatable(OpenAPIExtraSchemas.class)
@Retention(SOURCE)
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface OpenAPIExtraSchema {

    /**
     * @return Schema classes to include in generated Open API.
     */
    Class<?>[] value() default {};

    /**
     * @return Schema classes to include in generated Open API.
     */
    @AliasFor(member = "value")
    Class<?>[] classes() default {};

    /**
     * @return Schema classes to include in generated Open API.
     */
    @AliasFor(member = "value")
    String[] classNames() default {};

    /**
     * @return Schema classes annotated by OpenAPIExtraSchema and should be excluded.
     */
    Class<?>[] excludeClasses() default {};

    /**
     * @return Schema classes annotated by OpenAPIExtraSchema and should be excluded.
     */
    String[] excludeClassNames() default {};

    /**
     * @return packages with extra schemas should be included.
     * NOTE: Currently you can't use wildcard to include subpackages. Need to set every package in list
     */
    String[] packages() default {};

    /**
     * @return packages with extra schemas should be excluded.
     */
    String[] excludePackages() default {};
}
