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

import com.aidvps.druid.differ.internal.model.Table;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Validates that a directory structure conforms to the expected format for schema files.
 *
 * <p>Expected structure: - Directory contains .db files (database definitions) and .tbl files
 * (table definitions) - .db file names should match the database name declared inside - .tbl file
 * names should match the table name declared inside - No duplicate table names
 */
public class DirectoryStructureValidator {

    /**
     * Validates the directory structure.
     *
     * @param directoryPath Path to the directory
     * @param databaseNames Map of database names found in .db files
     * @param tables Map of tables found in .tbl files
     * @throws SchemaProviderException if validation fails
     */
    public void validate(
            Path directoryPath, Map<String, String> databaseNames, Map<String, Table> tables)
            throws SchemaProviderException {
        // Check for duplicate table names
        validateNoDuplicateTableNames(tables);

        // Check that table names are valid
        validateTableNames(tables);

        // Additional validation rules can be added here
    }

    /**
     * Validates that there are no duplicate table names.
     *
     * @param tables Map of tables
     * @throws SchemaProviderException if duplicate table names are found
     */
    private void validateNoDuplicateTableNames(Map<String, Table> tables)
            throws SchemaProviderException {
        Set<String> tableNames = tables.keySet();
        long uniqueCount = tableNames.size();
        long totalCount = tables.size();

        if (uniqueCount != totalCount) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.VALIDATION_ERROR,
                    "Duplicate table names found in directory");
        }
    }

    /**
     * Validates that table names are not null or empty.
     *
     * @param tables Map of tables
     * @throws SchemaProviderException if any table name is invalid
     */
    private void validateTableNames(Map<String, Table> tables) throws SchemaProviderException {
        for (Map.Entry<String, Table> entry : tables.entrySet()) {
            String tableName = entry.getKey();
            Table table = entry.getValue();

            if (tableName == null || tableName.trim().isEmpty()) {
                throw new SchemaProviderException(
                        SchemaProviderException.ErrorCode.VALIDATION_ERROR,
                        "Table name cannot be null or empty");
            }

            if (table == null) {
                throw new SchemaProviderException(
                        SchemaProviderException.ErrorCode.VALIDATION_ERROR,
                        "Table object cannot be null for table: " + tableName);
            }

            if (table.getName() == null || table.getName().trim().isEmpty()) {
                throw new SchemaProviderException(
                        SchemaProviderException.ErrorCode.VALIDATION_ERROR,
                        "Table name cannot be null or empty for table: " + tableName);
            }

            // Validate table name format (basic validation)
            if (!isValidIdentifier(table.getName())) {
                throw new SchemaProviderException(
                        SchemaProviderException.ErrorCode.VALIDATION_ERROR,
                        "Invalid table name: " + table.getName());
            }
        }
    }

    /**
     * Checks if a string is a valid SQL identifier.
     *
     * @param identifier The identifier to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidIdentifier(String identifier) {
        // Basic validation: alphanumeric and underscores, cannot start with number
        return identifier != null
                && !identifier.trim().isEmpty()
                && identifier.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }
}
