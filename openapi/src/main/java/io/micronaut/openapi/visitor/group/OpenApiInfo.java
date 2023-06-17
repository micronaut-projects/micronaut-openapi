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
package io.micronaut.openapi.visitor.group;

import io.micronaut.core.annotation.Internal;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Helpfull object with information about openAPI object and group / version settings.
 *
 * @since 4.9.2
 */
@Internal
public final class OpenApiInfo {

    private String version;
    private String groupName;
    private String groupTitle;
    private String filename;
    private OpenAPI openApi;
    private String specFilePath;

    public OpenApiInfo(OpenAPI openApi) {
        this.openApi = openApi;
    }

    public OpenApiInfo(String version, String groupName, String groupTitle, String filename, OpenAPI openApi) {
        this.version = version;
        this.groupName = groupName;
        this.groupTitle = groupTitle;
        this.filename = filename;
        this.openApi = openApi;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public OpenAPI getOpenApi() {
        return openApi;
    }

    public void setOpenApi(OpenAPI openApi) {
        this.openApi = openApi;
    }

    public String getSpecFilePath() {
        return specFilePath;
    }

    public void setSpecFilePath(String specFilePath) {
        this.specFilePath = specFilePath;
    }
}
