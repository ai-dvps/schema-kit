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

/** SQLite-specific database introspection enhancements. */
public class SQLiteIntrospector implements PlatformIntrospector {

    @Override
    public DatabasePlatform getPlatform() {
        return DatabasePlatform.SQLITE;
    }

    @Override
    public String getTableType(
            Connection connection, String catalog, String schema, String tableName)
            throws SQLException {
        // SQLite-specific table type detection
        return "TABLE";
    }

    @Override
    public String getColumnSpecialType(
            Connection connection,
            String catalog,
            String schema,
            String tableName,
            String columnName)
            throws SQLException {
        // SQLite-specific column type detection
        return null;
    }

    @Override
    public String normalizeDataType(String dataType) {
        // Normalize SQLite-specific type names
        if (dataType == null) {
            return null;
        }
        String normalized = dataType.toUpperCase();
        // SQLite has dynamic typing, but we can normalize common types
        if (normalized.equals("INT") || normalized.equals("INTEGER")) {
            return "INTEGER";
        } else if (normalized.equals("REAL")
                || normalized.equals("DOUBLE")
                || normalized.equals("FLOAT")) {
            return "REAL";
        } else if (normalized.equals("NUMERIC")) {
            return "NUMERIC";
        }
        return dataType;
    }

    @Override
    public boolean supportsFeature(PlatformFeature feature) {
        // SQLite doesn't support most advanced features
        switch (feature) {
            case AUTO_INCREMENT:
                // Only with AUTOINCREMENT keyword
                return true;
            case SEQUENCE:
            case IDENTITY:
            case JSON_TYPE:
            case ARRAY_TYPE:
            case FULLTEXT_INDEX:
            case SPATIAL_INDEX:
                return false;
            default:
                return false;
        }
    }
}
