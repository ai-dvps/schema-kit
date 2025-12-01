/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aidvps.schemakit.provider.dir;

import com.aidvps.schemakit.provider.ProviderType;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Configuration for DirectorySchemaProvider. */
public class DirectorySchemaProviderConfig implements SchemaProviderConfig {

    private final String directoryPath;
    private final boolean validateStructure;
    private final boolean followSymlinks;

    private DirectorySchemaProviderConfig(Builder builder) {
        this.directoryPath = builder.directoryPath;
        this.validateStructure = builder.validateStructure;
        this.followSymlinks = builder.followSymlinks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProviderType getType() {
        return ProviderType.DIRECTORY;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public boolean isValidateStructure() {
        return validateStructure;
    }

    public boolean isFollowSymlinks() {
        return followSymlinks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DirectorySchemaProviderConfig that = (DirectorySchemaProviderConfig) o;
        return validateStructure == that.validateStructure
                && followSymlinks == that.followSymlinks
                && Objects.equals(directoryPath, that.directoryPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directoryPath, validateStructure, followSymlinks);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("directoryPath", directoryPath);
        map.put("validateStructure", validateStructure);
        map.put("followSymlinks", followSymlinks);
        map.put("type", getType().name());
        return map;
    }

    @Override
    public String toString() {
        return "DirectorySchemaProviderConfig{"
                + "directoryPath='"
                + directoryPath
                + '\''
                + ", validateStructure="
                + validateStructure
                + ", followSymlinks="
                + followSymlinks
                + '}';
    }

    /** Builder for DirectorySchemaProviderConfig. */
    public static class Builder {

        private String directoryPath;
        private boolean validateStructure = true;
        private boolean followSymlinks = false;

        private Builder() {}

        public Builder directoryPath(String directoryPath) {
            this.directoryPath = directoryPath;
            return this;
        }

        public Builder validateStructure(boolean validateStructure) {
            this.validateStructure = validateStructure;
            return this;
        }

        public Builder followSymlinks(boolean followSymlinks) {
            this.followSymlinks = followSymlinks;
            return this;
        }

        public DirectorySchemaProviderConfig build() {
            if (directoryPath == null || directoryPath.trim().isEmpty()) {
                throw new IllegalArgumentException("directoryPath must not be null or empty");
            }
            return new DirectorySchemaProviderConfig(this);
        }
    }
}
