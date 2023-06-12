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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.micronaut.context.annotation.AliasFor;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotation can be used to add suffix and prefix for operationIds. For example, when you have
 * 2 controllers with same operations, but use generics:
 * <pre>
 * {@code @OpenAPIDecorator(opIdPrefix = "cats-", opIdSuffix = "-suffix")
 * @Controller("/cats")
 * interface MyCatsOperations extends Api<MyRequest, MyResponse> {
 * }
 *
 * @OpenAPIDecorator("dogs-")
 * @Controller("/dogs")
 * interface MyDogsOperations extends Api<MyRequest, MyResponse> {
 * }}
 * </pre>
 *
 * @since 4.5.0
 */
@Retention(RUNTIME)
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OpenAPIDecorator {

    /**
     * @return Prefix for operation ids.
     */
    String value() default "";

    /**
     * @return Prefix for operation ids.
     */
    @AliasFor(member = "value")
    String opIdPrefix() default "";

    /**
     * @return Suffix for operation ids.
     */
    String opIdSuffix() default "";

    /**
     * @return is this flag false, prefixes and suffixes will not be added to operationId
     * if operationId is set explicitly in the {@link io.swagger.v3.oas.annotations.Operation} annotation
     */
    boolean addAlways() default true;
}
