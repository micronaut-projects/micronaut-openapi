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

import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.openapi.visitor.group.OpenApiInfo;
import io.swagger.v3.oas.models.info.Info;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static io.micronaut.openapi.visitor.ConfigUtils.getConfigProperty;
import static io.micronaut.openapi.visitor.ContextUtils.calcClassesOutputPath;
import static io.micronaut.openapi.visitor.ContextUtils.calcProjectPath;
import static io.micronaut.openapi.visitor.ContextUtils.getClassesOutputPath;
import static io.micronaut.openapi.visitor.ContextUtils.visitMetaInfFile;
import static io.micronaut.openapi.visitor.ContextUtils.warn;
import static io.micronaut.openapi.visitor.OpenApiApplicationVisitor.replacePlaceholders;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_FILENAME;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_TARGET_FILE;
import static io.micronaut.openapi.visitor.OpenApiConfigProperty.MICRONAUT_OPENAPI_VIEWS_DEST_DIR;
import static io.micronaut.openapi.visitor.StringUtil.MINUS;
import static io.micronaut.openapi.visitor.StringUtil.PLACEHOLDER_PREFIX;
import static io.micronaut.openapi.visitor.StringUtil.SLASH;
import static io.micronaut.openapi.visitor.Utils.isKsp;

/**
 * File utilities methods.
 *
 * @since 4.10.0
 */
@Internal
public final class FileUtils {

    public static final String PROJECT_SCHEME = "project:";
    public static final String FILE_SCHEME = "file:";
    public static final String CLASSPATH_SCHEME = "classpath:";

    public static final String META_INF_DIR = "META-INF";
    public static final String DEFAULT_VIEWS_PATH = "swagger/views";
    public static final String DEFAULT_SPEC_PATH = "swagger/";
    public static final String EXT_ADOC = ".adoc";
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

    public static String normalizePath(String path) {
        return path.replace("\\\\", SLASH)
            .replace("\\", SLASH);
    }

    public static void createDirectories(Path f, VisitorContext context) {
        if (f.getParent() != null) {
            try {
                Files.createDirectories(f.getParent());
            } catch (IOException e) {
                warn("Fail to create directories for" + f + ": " + e.getMessage(), context);
            }
        }
    }

    public static boolean isYaml(String path) {
        return path.endsWith(EXT_YML) || path.endsWith(EXT_YAML);
    }

    public static boolean isJson(String path) {
        return path.endsWith(EXT_JSON);
    }

    public static Path getViewsDestDir(VisitorContext context) {
        String destDir = getConfigProperty(MICRONAUT_OPENAPI_VIEWS_DEST_DIR, context);
        if (StringUtils.isNotEmpty(destDir)) {
            Path destPath = resolve(context, Path.of(destDir));
            createDirectories(destPath, context);
            return destPath;
        }
        var classesOutputPath = getClassesOutputPath(context);
        if (classesOutputPath != null) {
            if (isKsp(context) && !classesOutputPath.endsWith(META_INF_DIR)) {
                classesOutputPath = classesOutputPath.resolve(META_INF_DIR);
            }
            classesOutputPath = classesOutputPath.resolve(DEFAULT_VIEWS_PATH);
        }
        return classesOutputPath;
    }

    public static Path getDefaultFilePath(String fileName, VisitorContext context) {
        var generatedFile = visitMetaInfFile(DEFAULT_SPEC_PATH + fileName, context);
        if (generatedFile != null) {
            var specPath = Path.of(generatedFile.toURI());
            createDirectories(specPath, context);
            // also trying to calculate classpath output directory and project directory if needed
            calcProjectPath(generatedFile, context);
            calcClassesOutputPath(generatedFile, context);
            return specPath;
        }
        warn("Unable to get swagger/" + fileName + " file.", context);
        return null;
    }

    public static Path openApiSpecFile(String fileName, VisitorContext context) {
        Path path = userDefinedSpecFile(context);
        if (path != null) {
            return path;
        }
        return getDefaultFilePath(fileName, context);
    }

    public static Path userDefinedSpecFile(VisitorContext context) {
        String targetFile = getConfigProperty(MICRONAUT_OPENAPI_TARGET_FILE, context);
        if (StringUtils.isEmpty(targetFile)) {
            return null;
        }
        Path specFile = resolve(context, Path.of(targetFile));
        createDirectories(specFile, context);
        return specFile;
    }

    public static Pair<String, String> calcFinalFilename(String groupFileName, OpenApiInfo openApiInfo, boolean isSingleGroup, String ext, VisitorContext context) {

        String fileName = "swagger" + ext;
        String documentTitle = "OpenAPI";

        Info info = openApiInfo.getOpenApi().getInfo();
        if (info != null) {
            documentTitle = info.getTitle() != null ? info.getTitle() : Environment.DEFAULT_NAME;
            documentTitle = documentTitle.toLowerCase(Locale.US).replace(' ', '-');
            String version = info.getVersion();
            if (version != null) {
                documentTitle = documentTitle + '-' + version;
            }
            fileName = documentTitle + ext;
        }

        String versionFromInfo = info != null && info.getVersion() != null ? info.getVersion() : StringUtils.EMPTY_STRING;

        String fileNameFromConfig = getConfigProperty(MICRONAUT_OPENAPI_FILENAME, context);
        if (StringUtils.isNotEmpty(fileNameFromConfig)) {
            fileName = replacePlaceholders(fileNameFromConfig, context) + ext;
            if (fileName.contains("${version}")) {
                fileName = fileName.replaceAll("\\$\\{version}", versionFromInfo);
            }
        }

        // construct filename for group
        if (!isSingleGroup) {
            if (StringUtils.isNotEmpty(groupFileName)) {
                fileName = groupFileName;
            } else {

                // default name: swagger-<version>-<groupName>-<apiVersion>

                fileName = fileName.substring(0, fileName.length() - ext.length())
                    + (openApiInfo.getGroupName() != null ? MINUS + openApiInfo.getGroupName() : StringUtils.EMPTY_STRING)
                    + (openApiInfo.getVersion() != null ? MINUS + openApiInfo.getVersion() : StringUtils.EMPTY_STRING);
            }

            fileName = replacePlaceholders(fileName, context) + ext;
            if (fileName.contains("${apiVersion}")) {
                fileName = fileName.replaceAll("\\$\\{apiVersion}", openApiInfo.getVersion() != null ? openApiInfo.getVersion() : versionFromInfo);
            }
            if (fileName.contains("${version}")) {
                fileName = fileName.replaceAll("\\$\\{version}", versionFromInfo);
            }
            if (fileName.contains("${group}")) {
                fileName = fileName.replaceAll("\\$\\{group}", openApiInfo.getGroupName() != null ? openApiInfo.getGroupName() : StringUtils.EMPTY_STRING);
            }
        }
        if (fileName.contains(PLACEHOLDER_PREFIX)) {
            warn("Can't set some placeholders in fileName: " + fileName, context);
        }

        return Pair.of(documentTitle, fileName);
    }

    public static String readFile(BufferedReader reader) throws IOException {
        var buf = new StringBuilder(1024);
        String line;
        while ((line = reader.readLine()) != null) {
            buf.append(line).append('\n');
        }
        return buf.toString();
    }
}
