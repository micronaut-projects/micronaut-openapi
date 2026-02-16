/*
 * Copyright 2017-2026 original authors
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

public class InvalidUserParameterException extends RuntimeException {
    private InvalidUserParameterException(String message) {
        super(message);
    }

    public static InvalidUserParameterException javaNoClass() {
        return new InvalidUserParameterException("User-defined authentication parameter class name option %s is required when %s is set to %s".formatted(JavaMicronautServerCodegen.OPT_USER_PARAMETER_CLASS, JavaMicronautServerCodegen.OPT_USER_PARAMETER_MODE, UserParameterMode.CUSTOM.name()));
    }

    public static InvalidUserParameterException kotlinNoClass() {
        return new InvalidUserParameterException("User-defined authentication parameter class name option %s is required when %s is set to %s".formatted(KotlinMicronautServerCodegen.OPT_USER_PARAMETER_CLASS, KotlinMicronautServerCodegen.OPT_USER_PARAMETER_MODE, UserParameterMode.CUSTOM.name()));
    }

    public static InvalidUserParameterException javaModeNotCustomButClassSet() {
        return new InvalidUserParameterException("User-defined authentication parameter class name option %s is not allowed when %s is not set to %s".formatted(JavaMicronautServerCodegen.OPT_USER_PARAMETER_CLASS, JavaMicronautServerCodegen.OPT_USER_PARAMETER_MODE, UserParameterMode.CUSTOM.name()));
    }

    public static InvalidUserParameterException kotlinModeNotCustomButClassSet() {
        return new InvalidUserParameterException("User-defined authentication parameter class name option %s is not allowed when %s is not set to %s".formatted(KotlinMicronautServerCodegen.OPT_USER_PARAMETER_CLASS, KotlinMicronautServerCodegen.OPT_USER_PARAMETER_MODE, UserParameterMode.CUSTOM.name()));
    }
}
