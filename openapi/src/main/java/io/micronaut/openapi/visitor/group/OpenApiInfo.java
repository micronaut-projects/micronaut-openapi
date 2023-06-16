package io.micronaut.openapi.visitor.group;

import io.swagger.v3.oas.models.OpenAPI;

public class OpenApiInfo {

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
