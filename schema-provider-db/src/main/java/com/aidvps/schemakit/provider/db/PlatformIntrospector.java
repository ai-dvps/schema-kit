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

import com.aidvps.druid.differ.internal.model.DatabasePlatform;
import java.sql.Connection;
import java.sql.SQLException;

/** Interface for platform-specific database introspection operations. */
public interface PlatformIntrospector {

    /**
     * Get the database platform this introspector supports.
     *
     * @return DatabasePlatform enum value
     */
    DatabasePlatform getPlatform();

    /**
     * Extract platform-specific table information.
     *
     * @param connection Database connection
     * @param catalog Catalog name
     * @param schema Schema name
     * @param tableName Table name
     * @return Platform-specific table metadata
     * @throws SQLException if extraction fails
     */
    String getTableType(Connection connection, String catalog, String schema, String tableName)
            throws SQLException;

    /**
     * Extract platform-specific column information.
     *
     * @param connection Database connection
     * @param catalog Catalog name
     * @param schema Schema name
     * @param tableName Table name
     * @param columnName Column name
     * @return Platform-specific column metadata
     * @throws SQLException if extraction fails
     */
    String getColumnSpecialType(
            Connection connection,
            String catalog,
            String schema,
            String tableName,
            String columnName)
            throws SQLException;

    /**
     * Normalize data type for this platform.
     *
     * @param dataType Raw data type from metadata
     * @return Normalized data type
     */
    String normalizeDataType(String dataType);

    /**
     * Check if platform supports specific features.
     *
     * @param feature Feature to check
     * @return true if supported
     */
    boolean supportsFeature(PlatformFeature feature);

    /** Enum for platform-specific features. */
    enum PlatformFeature {
        AUTO_INCREMENT,
        SEQUENCE,
        IDENTITY,
        JSON_TYPE,
        ARRAY_TYPE,
        FULLTEXT_INDEX,
        SPATIAL_INDEX
    }
}
