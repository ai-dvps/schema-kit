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
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/** Resolves database dialect/platform from JDBC connection metadata. */
public class DialectResolver {

    /**
     * Resolve database platform from connection metadata.
     *
     * @param connection Database connection
     * @return DatabasePlatform enum value
     * @throws SQLException if metadata extraction fails
     * @throws SchemaProviderException if platform cannot be determined
     */
    public DatabasePlatform resolve(Connection connection)
            throws SQLException, SchemaProviderException {
        DatabaseMetaData metaData = connection.getMetaData();
        String productName = metaData.getDatabaseProductName().toUpperCase(Locale.ROOT);
        String productVersion = metaData.getDatabaseProductVersion();

        // Determine platform based on product name
        if (productName.contains("MYSQL")) {
            // Check if it's actually MariaDB (MySQL-compatible but reports as MariaDB)
            if (productName.contains("MARIADB")) {
                return DatabasePlatform.MARIADB;
            }
            return DatabasePlatform.MYSQL;
        } else if (productName.contains("POSTGRESQL") || productName.contains("POSTGRE")) {
            return DatabasePlatform.POSTGRESQL;
        } else if (productName.contains("SQLITE")) {
            return DatabasePlatform.SQLITE;
        } else if (productName.contains("MARIADB")) {
            return DatabasePlatform.MARIADB;
        } else if (productName.contains("H2")) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.UNKNOWN_ERROR,
                    "H2 database is not yet supported",
                    null);
        } else if (productName.contains("ORACLE")) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.UNKNOWN_ERROR,
                    "Oracle database is not yet supported",
                    null);
        } else if (productName.contains("SQL SERVER") || productName.contains("MICROSOFT")) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.UNKNOWN_ERROR,
                    "SQL Server database is not yet supported",
                    null);
        }

        throw new SchemaProviderException(
                SchemaProviderException.ErrorCode.UNKNOWN_ERROR,
                "Unsupported database platform: " + productName,
                null);
    }

    /**
     * Check if a platform is supported.
     *
     * @param platform Database platform
     * @return true if supported
     */
    public boolean isSupported(DatabasePlatform platform) {
        return platform == DatabasePlatform.MYSQL
                || platform == DatabasePlatform.POSTGRESQL
                || platform == DatabasePlatform.MARIADB
                || platform == DatabasePlatform.SQLITE;
    }
}
