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

import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.schemakit.provider.ProviderType;
import com.aidvps.schemakit.provider.SchemaProvider;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SchemaProvider implementation for JAR archive sources.
 *
 * <p>Supports extracting schema files from JAR archives, both from file system and classpath. Works
 * with .db and .tbl files embedded in JARs.
 */
public class JarSchemaProvider implements SchemaProvider {

    private final JarResourceExtractor resourceExtractor;

    public JarSchemaProvider() {
        this.resourceExtractor = new JarResourceExtractor();
    }

    // Constructor for testing
    JarSchemaProvider(JarResourceExtractor resourceExtractor) {
        this.resourceExtractor = resourceExtractor;
    }

    @Override
    public ProviderType getType() {
        return ProviderType.JAR;
    }

    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        if (config == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }

        if (!(config instanceof JarSchemaProviderConfig)) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Configuration must be JarSchemaProviderConfig");
        }

        JarSchemaProviderConfig jarConfig = (JarSchemaProviderConfig) config;

        // Validate configuration
        validateConfig(jarConfig);

        try {
            // TODO: Implement full JAR resource extraction and schema parsing
            // For now, throw UnsupportedOperationException while dependencies are being built
            throw new UnsupportedOperationException(
                    "JarSchemaProvider implementation in progress. Full schema extraction "
                            + "will be available once supporting classes are complete.");
        } catch (Exception e) {
            if (e instanceof SchemaProviderException) {
                throw e;
            }
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                    "Failed to extract schema from JAR: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Validate the JAR provider configuration.
     *
     * @param config JAR configuration to validate
     * @throws SchemaProviderException if configuration is invalid
     */
    private void validateConfig(JarSchemaProviderConfig config) throws SchemaProviderException {
        String jarPath = config.getJarPath();

        if (jarPath == null || jarPath.trim().isEmpty()) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "JAR path must not be null or empty");
        }

        // Check if JAR path is valid
        try {
            Path path = Paths.get(jarPath);
            // JAR path will be validated by JarResourceExtractor
        } catch (Exception e) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Invalid JAR path: " + jarPath);
        }

        // Validate that if resourcePath is provided, it's not empty
        String resourcePath = config.getResourcePath();
        if (resourcePath != null && resourcePath.trim().isEmpty()) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Resource path must not be empty if specified");
        }
    }
}
