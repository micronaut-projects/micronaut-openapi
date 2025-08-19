/*
 * Copyright 2017-2025 original authors
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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotation can be used as alternative to io.micronaut.http.annotation.RequestBean
 * to mark parameter as wrapped request details. It useful, when you haven't micronaut-http in dependencies.
 * For example, when you use micronaut-openapi with spring-boot application.
 *
 * @since 6.18.0
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Inherited
public @interface OpenAPIRequest {
}
