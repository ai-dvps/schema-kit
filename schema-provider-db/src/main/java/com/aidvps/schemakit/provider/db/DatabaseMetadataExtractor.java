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

package com.aidvps.schemakit.provider.db;

import com.aidvps.druid.differ.DatabaseDialect;
import com.aidvps.druid.differ.internal.model.Column;
import com.aidvps.druid.differ.internal.model.DatabasePlatform;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.Table;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Extracts database metadata using JDBC DatabaseMetaData. */
public class DatabaseMetadataExtractor {

    private final DatabasePlatform platform;

    public DatabaseMetadataExtractor(DatabasePlatform platform) {
        this.platform = platform;
    }

    public DatabaseMetadataExtractor() {
        this.platform = null;
    }

    /**
     * Convert DatabasePlatform to DatabaseDialect.
     *
     * @param platform DatabasePlatform
     * @return DatabaseDialect
     */
    private DatabaseDialect convertToDialect(DatabasePlatform platform) {
        if (platform == null) {
            return DatabaseDialect.MYSQL;
        }
        switch (platform) {
            case MYSQL:
            case MARIADB:
                return DatabaseDialect.MYSQL;
            case POSTGRESQL:
                return DatabaseDialect.POSTGRESQL;
            case SQLITE:
                // SQLite doesn't have a corresponding DatabaseDialect, use MYSQL as closest
                return DatabaseDialect.MYSQL;
            default:
                return DatabaseDialect.MYSQL;
        }
    }

    /**
     * Extract schema from database connection.
     *
     * @param connection Database connection
     * @param platform Database platform
     * @return Schema object
     * @throws SchemaProviderException if extraction fails
     */
    public Schema extractSchema(Connection connection, DatabasePlatform platform)
            throws SchemaProviderException {
        try {
            DatabaseMetaData metaData = connection.getMetaData();

            // Get all tables
            Map<String, Table> tables = new HashMap<>();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[] {"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String tableSchema = rs.getString("TABLE_SCHEM");
                    String tableCatalog = rs.getString("TABLE_CAT");

                    // Extract columns for this table
                    Map<String, Column> columns =
                            extractColumns(metaData, tableCatalog, tableSchema, tableName);

                    // Build table object
                    Table.Builder tableBuilder = Table.builder(tableName);
                    for (Column column : columns.values()) {
                        tableBuilder.addColumn(column);
                    }
                    Table table = tableBuilder.build();

                    tables.put(tableName, table);
                }
            }

            // Build schema object
            DatabaseDialect dialect = convertToDialect(platform);
            Schema.Builder schemaBuilder = Schema.builder(dialect);
            for (Table table : tables.values()) {
                schemaBuilder.addTable(table);
            }
            return schemaBuilder.build();

        } catch (SQLException e) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.PARSE_ERROR,
                    "Failed to extract database metadata: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Extract schema from database connection.
     *
     * @param connection Database connection
     * @return Schema object
     * @throws SchemaProviderException if extraction fails
     */
    public Schema extractSchema(Connection connection) throws SchemaProviderException {
        if (platform == null) {
            throw new IllegalStateException("Platform must be set before extracting schema");
        }
        return extractSchema(connection, platform);
    }

    /**
     * Extract columns for a specific table.
     *
     * @param metaData Database metadata
     * @param catalog Catalog name
     * @param schema Schema name
     * @param tableName Table name
     * @return Map of column name to Column object
     * @throws SQLException if extraction fails
     */
    protected Map<String, Column> extractColumns(
            DatabaseMetaData metaData, String catalog, String schema, String tableName)
            throws SQLException {
        Map<String, Column> columns = new HashMap<>();

        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                String defaultValue = rs.getString("COLUMN_DEF");
                String remarks = rs.getString("REMARKS");

                Column.Builder columnBuilder = Column.builder(columnName, dataType);
                if (columnSize > 0) {
                    columnBuilder.length(columnSize);
                }
                if (decimalDigits > 0) {
                    // Use precision to set scale for decimal types
                    columnBuilder.precision(columnSize > 0 ? columnSize : 10, decimalDigits);
                }
                columnBuilder.nullable(nullable);
                if (defaultValue != null) {
                    columnBuilder.defaultValue(defaultValue);
                }
                if (remarks != null) {
                    columnBuilder.comment(remarks);
                }
                Column column = columnBuilder.build();

                columns.put(columnName, column);
            }
        }

        return columns;
    }
}
