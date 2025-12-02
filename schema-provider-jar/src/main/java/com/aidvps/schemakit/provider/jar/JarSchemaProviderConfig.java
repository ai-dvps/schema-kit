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

package com.aidvps.schemakit.provider.jar;

import com.aidvps.schemakit.provider.ProviderType;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for JarSchemaProvider.
 *
 * <p>Supports configuration for extracting schema files from JAR archives. Supports both file
 * system JARs and JARs on the classpath.
 */
public class JarSchemaProviderConfig implements SchemaProviderConfig {

    private final String jarPath;
    private final String resourcePath;
    private final boolean extractToTemporary;
    private final boolean validateJar;

    private JarSchemaProviderConfig(Builder builder) {
        this.jarPath = builder.jarPath;
        this.resourcePath = builder.resourcePath;
        this.extractToTemporary = builder.extractToTemporary;
        this.validateJar = builder.validateJar;
    }

    /**
     * Create a new builder.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public ProviderType getType() {
        return ProviderType.JAR;
    }

    /**
     * Get the JAR file path. Can be an absolute path, relative path, or classpath resource path.
     *
     * @return JAR file path
     */
    public String getJarPath() {
        return jarPath;
    }

    /**
     * Get the resource path within the JAR where schema files are located. If null, searches the
     * entire JAR.
     *
     * @return Resource path, may be null
     */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * Check if JAR should be extracted to a temporary location.
     *
     * @return true if extracting to temporary location, false otherwise
     */
    public boolean isExtractToTemporary() {
        return extractToTemporary;
    }

    /**
     * Check if JAR structure should be validated.
     *
     * @return true if validating JAR, false otherwise
     */
    public boolean isValidateJar() {
        return validateJar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JarSchemaProviderConfig that = (JarSchemaProviderConfig) o;
        return extractToTemporary == that.extractToTemporary
                && validateJar == that.validateJar
                && Objects.equals(jarPath, that.jarPath)
                && Objects.equals(resourcePath, that.resourcePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jarPath, resourcePath, extractToTemporary, validateJar);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("jarPath", jarPath);
        map.put("resourcePath", resourcePath);
        map.put("extractToTemporary", extractToTemporary);
        map.put("validateJar", validateJar);
        map.put("type", getType().name());
        return map;
    }

    @Override
    public String toString() {
        return "JarSchemaProviderConfig{"
                + "jarPath='"
                + jarPath
                + '\''
                + ", resourcePath='"
                + resourcePath
                + '\''
                + ", extractToTemporary="
                + extractToTemporary
                + ", validateJar="
                + validateJar
                + '}';
    }

    /** Builder for JarSchemaProviderConfig. */
    public static class Builder {

        private String jarPath;
        private String resourcePath;
        private boolean extractToTemporary = true;
        private boolean validateJar = true;

        private Builder() {}

        /**
         * Set the JAR file path. Can be an absolute path, relative path, or classpath resource
         * path.
         *
         * @param jarPath JAR file path
         * @return This builder
         */
        public Builder jarPath(String jarPath) {
            this.jarPath = jarPath;
            return this;
        }

        /**
         * Set the resource path within the JAR where schema files are located. If null, searches
         * the entire JAR.
         *
         * @param resourcePath Resource path within JAR
         * @return This builder
         */
        public Builder resourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
            return this;
        }

        /**
         * Set whether to extract JAR to a temporary location. Defaults to true.
         *
         * @param extractToTemporary true to extract to temporary location, false otherwise
         * @return This builder
         */
        public Builder extractToTemporary(boolean extractToTemporary) {
            this.extractToTemporary = extractToTemporary;
            return this;
        }

        /**
         * Set whether to validate JAR structure. Defaults to true.
         *
         * @param validateJar true to validate JAR, false otherwise
         * @return This builder
         */
        public Builder validateJar(boolean validateJar) {
            this.validateJar = validateJar;
            return this;
        }

        /**
         * Build the JarSchemaProviderConfig.
         *
         * @return JarSchemaProviderConfig instance
         * @throws IllegalArgumentException if jarPath is null or empty
         */
        public JarSchemaProviderConfig build() {
            if (jarPath == null || jarPath.trim().isEmpty()) {
                throw new IllegalArgumentException("jarPath must not be null or empty");
            }
            return new JarSchemaProviderConfig(this);
        }
    }
}
