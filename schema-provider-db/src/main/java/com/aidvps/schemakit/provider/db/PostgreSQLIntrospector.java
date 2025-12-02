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

/** PostgreSQL-specific database introspection enhancements. */
public class PostgreSQLIntrospector implements PlatformIntrospector {

    @Override
    public DatabasePlatform getPlatform() {
        return DatabasePlatform.POSTGRESQL;
    }

    @Override
    public String getTableType(
            Connection connection, String catalog, String schema, String tableName)
            throws SQLException {
        // PostgreSQL-specific table type detection
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
        // PostgreSQL-specific column type detection
        return null;
    }

    @Override
    public String normalizeDataType(String dataType) {
        // Normalize PostgreSQL-specific type names
        if (dataType == null) {
            return null;
        }
        String normalized = dataType.toUpperCase();
        // Handle PostgreSQL-specific type aliases
        if (normalized.equals("INT4")) {
            return "INTEGER";
        } else if (normalized.equals("INT8")) {
            return "BIGINT";
        } else if (normalized.equals("INT2")) {
            return "SMALLINT";
        } else if (normalized.equals("FLOAT8")) {
            return "DOUBLE PRECISION";
        }
        return dataType;
    }

    @Override
    public boolean supportsFeature(PlatformFeature feature) {
        switch (feature) {
            case SEQUENCE:
            case IDENTITY:
            case JSON_TYPE:
            case ARRAY_TYPE:
            case SPATIAL_INDEX:
                return true;
            case AUTO_INCREMENT:
            case FULLTEXT_INDEX:
                return false;
            default:
                return false;
        }
    }
}
