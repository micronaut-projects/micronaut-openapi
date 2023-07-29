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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.visitor.VisitorContext;

/**
 * File utilities methods.
 *
 * @since 4.10.0
 */
@Internal
public final class FileUtils {

    public static final String EXT_YML = ".yml";
    public static final String EXT_YAML = ".yaml";
    public static final String EXT_JSON = ".json";

    private FileUtils() {
    }

    public static Path resolve(VisitorContext context, Path path) {
        if (!path.isAbsolute() && context != null) {
            Path projectPath = ConfigUtils.getProjectPath(context);
            if (projectPath != null) {
                path = projectPath.resolve(path);
            }
        }
        return path.toAbsolutePath();
    }

    public static void createDirectories(Path f, VisitorContext context) {
        if (f.getParent() != null) {
            try {
                Files.createDirectories(f.getParent());
            } catch (IOException e) {
                context.warn("Fail to create directories for" + f + ": " + e.getMessage(), null);
            }
        }
    }

    public static boolean isYaml(String path) {
        return path.endsWith(EXT_YML) || path.endsWith(EXT_YAML);
    }
}
