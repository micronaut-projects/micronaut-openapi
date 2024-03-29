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
package io.micronaut.openapi.adoc.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * File utilities methods.
 *
 * @since 5.2.0
 */
public final class FileUtils {

    public static final String FILE_SCHEME = "file:";
    public static final String CLASSPATH_SCHEME = "classpath:";

    private FileUtils() {
    }

    public static String loadFileFromClasspath(String location) {
        String file = location.replace("\\\\", "/");

        var inputStream = FileUtils.class.getResourceAsStream(file);
        if (inputStream == null) {
            inputStream = FileUtils.class.getClassLoader().getResourceAsStream(file);
        }
        if (inputStream == null) {
            inputStream = ClassLoader.getSystemResourceAsStream(file);
        }

        if (inputStream != null) {
            try {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + file + " from the classpath", e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    //
                }
            }
        }
        throw new RuntimeException("Could not find " + file + " on the classpath");
    }
}
