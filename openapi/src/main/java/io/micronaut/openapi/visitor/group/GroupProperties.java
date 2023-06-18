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

import java.util.List;

import io.micronaut.core.annotation.Internal;

/**
 * OpenAPI group properties.
 *
 * @since 4.9.2
 */
@Internal
public final class GroupProperties {

    /**
     * Group name.
     */
    private final String name;
    /**
     * Group title for swagger-ui selector.
     */
    private String displayName;
    /**
     * Is this group primary for swagger-ui.
     */
    private Boolean primary;
    /**
     * Is this group exclude common endpoints.
     */
    private Boolean commonExclude;
    /**
     * Group final swagger filename.
     */
    private String filename;
    /**
     * Packages included to this group. Override annotation configuration.
     */
    private List<PackageProperties> packages;
    /**
     * Packages excluded from this group. Override annotation configuration.
     */
    private List<PackageProperties> packagesExclude;

    public GroupProperties(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<PackageProperties> getPackages() {
        return packages;
    }

    public void setPackages(List<PackageProperties> packages) {
        this.packages = packages;
    }

    public List<PackageProperties> getPackagesExclude() {
        return packagesExclude;
    }

    public void setPackagesExclude(List<PackageProperties> packagesExclude) {
        this.packagesExclude = packagesExclude;
    }

    public Boolean getCommonExclude() {
        return commonExclude;
    }

    public void setCommonExclude(Boolean commonExclude) {
        this.commonExclude = commonExclude;
    }

    /**
     * Package name with inclusion subpackasges flag.
     *
     * @since 4.9.2
     */
    @Internal
    public static final class PackageProperties {

        private final String name;
        private final boolean includeSubpackages;

        public PackageProperties(String name, boolean includeSubpackages) {
            this.name = name;
            this.includeSubpackages = includeSubpackages;
        }

        public String getName() {
            return name;
        }

        public boolean isIncludeSubpackages() {
            return includeSubpackages;
        }
    }
}
