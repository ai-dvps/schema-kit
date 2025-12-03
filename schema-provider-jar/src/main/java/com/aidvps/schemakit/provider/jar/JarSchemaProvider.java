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

import com.aidvps.druid.differ.DatabaseDialect;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.schemakit.provider.ProviderType;
import com.aidvps.schemakit.provider.SchemaProvider;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

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
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Configuration must not be null");
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
            String jarPath = jarConfig.getJarPath();
            String resourcePath = jarConfig.getResourcePath();

            // Extract schema resources from JAR
            Map<String, String> schemaResources =
                    resourceExtractor.extractSchemaResources(jarPath, resourcePath);

            String jarName = Paths.get(jarPath).getFileName().toString();

            // Get dialect from config, default to MYSQL if not specified
            DatabaseDialect dialect = jarConfig.getDialect();
            if (dialect == null) {
                dialect = DatabaseDialect.MYSQL;
            }

            // Parse extracted resources into a schema
            Schema.Builder schemaBuilder = Schema.builder(dialect);

            if (schemaResources.isEmpty()) {
                // No schema files found, return empty schema
                return schemaBuilder
                        .addMetadata("source", jarName.replace(".jar", "") + "_schema")
                        .build();
            }

            // Parse each extracted resource
            for (Map.Entry<String, String> entry : schemaResources.entrySet()) {
                String resourceName = entry.getKey();
                String content = entry.getValue();

                try {
                    parseResource(resourceName, content, schemaBuilder);
                } catch (Exception e) {
                    // Log warning but continue processing other resources
                    System.err.println(
                            "Warning: Failed to parse resource "
                                    + resourceName
                                    + ": "
                                    + e.getMessage());
                }
            }

            return schemaBuilder
                    .addMetadata("source", jarName.replace(".jar", "") + "_schema")
                    .build();
        } catch (Exception e) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                    "Failed to extract schema from JAR: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Parse a schema resource and add tables to the schema builder.
     *
     * @param resourceName Name of the resource
     * @param content Content of the resource
     * @param schemaBuilder Schema builder to add tables to
     * @throws Exception if parsing fails
     */
    private void parseResource(String resourceName, String content, Schema.Builder schemaBuilder)
            throws Exception {
        String lowerName = resourceName.toLowerCase();

        try {
            // Simple parsing logic based on file extension
            if (lowerName.endsWith(".db") || lowerName.endsWith(".tbl")) {
                parseTableFile(resourceName, content, schemaBuilder);
            } else {
                // Unknown file type, add to metadata
                schemaBuilder.addMetadata("resource:" + resourceName, "Parsed as text");
            }
        } catch (Exception e) {
            throw new Exception("Failed to parse resource: " + resourceName, e);
        }
    }

    /**
     * Parse a table file content (DB or TBL format).
     *
     * @param resourceName Name of the resource
     * @param content File content
     * @param schemaBuilder Schema builder
     */
    private void parseTableFile(String resourceName, String content, Schema.Builder schemaBuilder) {
        // Simple parsing for table files
        // These files typically contain table definitions in a simpler format

        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                // Add line to metadata as a simple table reference
                // In full implementation, parse the format properly
                schemaBuilder.addMetadata(
                        "table:" + line, "Table file parsed from " + resourceName);
            }
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
