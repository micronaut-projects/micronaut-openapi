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

/**
 * Supported user parameter modes.
 */
public enum UserParameterMode {

    NONE(null, null, null, null),
    PRINCIPAL("principal", "The principal", "Principal", "java.security.Principal"),
    AUTHENTICATION("authentication", "The authentication", "Authentication", "io.micronaut.security.authentication.Authentication"),
    ;

    private final String paramName;
    private final String paramDescription;
    private final String className;
    private final String classFullName;

    UserParameterMode(String paramName, String paramDescription, String className, String classFullName) {
        this.paramName = paramName;
        this.paramDescription = paramDescription;
        this.className = className;
        this.classFullName = classFullName;
    }

    public String getParamName() {
        return paramName;
    }

    public String getParamDescription() {
        return paramDescription;
    }

    public String getClassName() {
        return className;
    }

    public String getClassFullName() {
        return classFullName;
    }
}
