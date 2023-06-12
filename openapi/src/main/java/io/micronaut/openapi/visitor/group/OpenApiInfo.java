package io.micronaut.openapi.visitor.group;

import io.swagger.v3.oas.models.OpenAPI;

public class OpenApiInfo {

    private String version;
    private String groupName;
    private String groupTitle;
    private String filename;
    private OpenAPI openApi;

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

    public String getGroupName() {
        return groupName;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public String getFilename() {
        return filename;
    }

    public OpenAPI getOpenApi() {
        return openApi;
    }
}
