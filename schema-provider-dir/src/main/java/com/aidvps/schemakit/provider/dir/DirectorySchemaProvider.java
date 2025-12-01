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

import com.aidvps.druid.differ.DatabaseDialect;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.Table;
import com.aidvps.schemakit.provider.ProviderType;
import com.aidvps.schemakit.provider.SchemaProvider;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/** SchemaProvider implementation for directory-based file sources (.db and .tbl files). */
public class DirectorySchemaProvider implements SchemaProvider {

    private final DatabaseFileParser databaseFileParser;
    private final TableFileParser tableFileParser;
    private final DirectoryStructureValidator structureValidator;

    public DirectorySchemaProvider() {
        this.databaseFileParser = new DatabaseFileParser();
        this.tableFileParser = new TableFileParser();
        this.structureValidator = new DirectoryStructureValidator();
    }

    // Constructor for testing
    DirectorySchemaProvider(
            DatabaseFileParser databaseFileParser,
            TableFileParser tableFileParser,
            DirectoryStructureValidator structureValidator) {
        this.databaseFileParser = databaseFileParser;
        this.tableFileParser = tableFileParser;
        this.structureValidator = structureValidator;
    }

    @Override
    public ProviderType getType() {
        return ProviderType.DIRECTORY;
    }

    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        if (config == null) {
            throw new NullPointerException("Configuration must not be null");
        }

        if (!(config instanceof DirectorySchemaProviderConfig)) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Configuration must be DirectorySchemaProviderConfig");
        }

        DirectorySchemaProviderConfig dirConfig = (DirectorySchemaProviderConfig) config;

        try {
            validateConfig(dirConfig);
        } catch (SchemaProviderException e) {
            throw e;
        }

        Path directoryPath = Paths.get(dirConfig.getDirectoryPath());

        if (!Files.exists(directoryPath)) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.SOURCE_NOT_FOUND,
                    "Directory not found: " + dirConfig.getDirectoryPath());
        }

        if (!Files.isDirectory(directoryPath)) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.SOURCE_NOT_FOUND,
                    "Path is not a directory: " + dirConfig.getDirectoryPath());
        }

        try {
            // Parse all .db files to extract database definitions
            Map<String, String> databaseNames =
                    databaseFileParser.parseDatabaseFiles(directoryPath);

            // Parse all .tbl files to extract table definitions
            Map<String, Table> tables = tableFileParser.parseTableFiles(directoryPath);

            // Optional: validate directory structure
            if (dirConfig.isValidateStructure()) {
                structureValidator.validate(directoryPath, databaseNames, tables);
            }

            // For now, we'll create a schema with all tables from all "databases"
            // In a real implementation, we might want to separate by database
            // This is a simplified approach for the MVP

            // Create a schema builder - we need to determine the dialect
            // For now, use MYSQL as default, but this should be configurable
            DatabaseDialect dialect = DatabaseDialect.MYSQL;

            Schema.Builder schemaBuilder = Schema.builder(dialect);

            // Add all tables to the schema
            for (Table table : tables.values()) {
                schemaBuilder.addTable(table);
            }

            return schemaBuilder.build();

        } catch (IOException e) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.PARSE_ERROR,
                    "Error reading directory: " + dirConfig.getDirectoryPath(),
                    e);
        } catch (Exception e) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.PARSE_ERROR,
                    "Error parsing schema from directory: " + dirConfig.getDirectoryPath(),
                    e);
        }
    }

    @Override
    public void validateConfig(SchemaProviderConfig config) throws SchemaProviderException {
        if (config == null) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Configuration must not be null");
        }

        if (!(config instanceof DirectorySchemaProviderConfig)) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Configuration must be DirectorySchemaProviderConfig");
        }

        DirectorySchemaProviderConfig dirConfig = (DirectorySchemaProviderConfig) config;

        if (dirConfig.getDirectoryPath() == null || dirConfig.getDirectoryPath().trim().isEmpty()) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Directory path must not be null or empty");
        }
    }
}
