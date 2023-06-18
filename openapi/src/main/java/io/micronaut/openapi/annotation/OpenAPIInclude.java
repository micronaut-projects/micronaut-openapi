/*
 * Copyright 2017-2020 original authors
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotation can be used to include additional io.micronaut.http.annotation.Controller or
 * io.micronaut.management.endpoint.annotation.Endpoint classes to be processed for OpenAPI definition.
 * This is useful in cases where you cannot alter the source code and wish to generate Open API for already compiled classes.
 *
 * @author Denis Stepanov
 */
@Repeatable(OpenAPIIncludes.class)
@Retention(RUNTIME)
@Documented
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface OpenAPIInclude {

    /**
     * @return The classes to generate Open API for.
     */
    Class<?>[] value() default {};

    /**
     * @return The classes to generate Open API for.
     */
    @AliasFor(member = "value")
    Class<?>[] classes() default {};

    /**
     * @return The classes to generate Open API for.
     */
    @AliasFor(member = "value")
    String[] classNames() default {};

    /**
     * @return Array of groups to which this controller should be included.
     *
     * @since 4.9.2
     */
    String[] groups() default {};

    /**
     * @return Array of groups to which this controller should not be included.
     *
     * @since 4.9.2
     */
    String[] groupsExclude() default {};

    /**
     * @return Custom URI for controller
     *
     * @since 4.4.0
     */
    String uri() default "";

    /**
     * A list of tags used by the specification with additional metadata.
     * The order of the tags can be used to reflect on their order by the parsing tools.
     *
     * @return the tags used by the specification with any additional metadata
     */
    Tag[] tags() default {};

    /**
     * A declaration of which security mechanisms can be used across the API.
     *
     * @return the array of servers used for this API
     */
    SecurityRequirement[] security() default {};
}
