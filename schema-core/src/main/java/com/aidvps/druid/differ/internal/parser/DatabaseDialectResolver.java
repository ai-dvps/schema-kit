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

package com.aidvps.druid.differ.internal.parser;

import com.aidvps.druid.differ.DatabaseDialect;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves DatabaseDialect to druid-parser's DbType.
 *
 * <p>This utility class provides a mapping from our internal DatabaseDialect enum to the
 * database-specific types used by the druid-parser library for SQL parsing and generation.
 */
public class DatabaseDialectResolver {

    private static final Map<DatabaseDialect, String> DIALECT_TO_DBTYPE = new HashMap<>();

    static {
        DIALECT_TO_DBTYPE.put(DatabaseDialect.MYSQL, "mysql");
        DIALECT_TO_DBTYPE.put(DatabaseDialect.POSTGRESQL, "postgresql");
        DIALECT_TO_DBTYPE.put(DatabaseDialect.ORACLE, "oracle");
    }

    private DatabaseDialectResolver() {
        // Utility class - prevent instantiation
    }

    /**
     * Resolves a DatabaseDialect to the corresponding druid-parser DbType string.
     *
     * @param dialect the DatabaseDialect to resolve
     * @return the corresponding DbType string (e.g., "mysql", "postgresql", "oracle")
     * @throws IllegalArgumentException if the dialect is not recognized
     */
    public static String resolveDbType(DatabaseDialect dialect) {
        if (dialect == null) {
            throw new IllegalArgumentException("Database dialect cannot be null");
        }

        String dbType = DIALECT_TO_DBTYPE.get(dialect);
        if (dbType == null) {
            throw new IllegalArgumentException("Unsupported database dialect: " + dialect);
        }

        return dbType;
    }

    /**
     * Checks if a DatabaseDialect is supported.
     *
     * @param dialect the dialect to check
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(DatabaseDialect dialect) {
        return DIALECT_TO_DBTYPE.containsKey(dialect);
    }
}
