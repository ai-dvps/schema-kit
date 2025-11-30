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
import com.aidvps.druid.differ.exception.SchemaParsingException;
import com.aidvps.druid.differ.internal.cache.SchemaCache;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.sql.ast.statement.SQLCreateTableStatement;
import com.aidvps.druid.sql.dialect.mysql.parser.MySqlCreateTableParser;
import com.aidvps.druid.sql.dialect.oracle.parser.OracleCreateTableParser;
import com.aidvps.druid.sql.dialect.postgresql.parser.PGCreateTableParser;
import com.aidvps.druid.sql.parser.SQLCreateTableParser;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for druid-parser to convert SQL statements to internal Schema model.
 *
 * <p>This class wraps the druid-parser's SQLCreateTableParser and provides a clean interface for
 * parsing CREATE TABLE statements into our internal Schema representation.
 */
public class DruidParserAdapter {

    private final String dbType;
    private final SchemaExtractor schemaExtractor;
    private final SchemaCache cache;
    private final MessageDigest digest;

    /**
     * Creates a new DruidParserAdapter.
     *
     * @param dbType the database type (e.g., "mysql", "postgresql", "oracle")
     */
    public DruidParserAdapter(String dbType) {
        this(dbType, 100); // Default cache size
    }

    /**
     * Creates a new DruidParserAdapter with a custom cache size.
     *
     * @param dbType the database type (e.g., "mysql", "postgresql", "oracle")
     * @param cacheSize the maximum number of schemas to cache
     */
    public DruidParserAdapter(String dbType, int cacheSize) {
        this.dbType = dbType;
        this.schemaExtractor = new SchemaExtractor();
        this.cache = new SchemaCache(cacheSize);
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SHA-256 digest", e);
        }
    }

    /**
     * Parses a SQL schema definition into a Schema object.
     *
     * @param sql the SQL schema definition (may contain multiple CREATE TABLE statements)
     * @return a Schema object representing the parsed schema
     * @throws SchemaParsingException if the SQL cannot be parsed
     */
    public Schema parseSchema(String sql) throws SchemaParsingException {
        if (sql == null || sql.trim().isEmpty()) {
            // Return empty schema for empty/null SQL
            return Schema.builder(DatabaseDialect.MYSQL).build();
        }

        // Compute hash for caching
        String hash = computeHash(sql);

        // Check cache first
        Schema cached = cache.get(hash);
        if (cached != null) {
            return cached;
        }

        // Parse and cache
        try {
            List<SQLCreateTableStatement> tableStatements = extractCreateTableStatements(sql);
            Schema schema = schemaExtractor.extract(tableStatements, dbType);

            // Store in cache
            cache.put(hash, schema);

            return schema;
        } catch (Exception e) {
            if (e instanceof SchemaParsingException) {
                throw e;
            }
            throw new SchemaParsingException(
                    "Failed to parse schema: " + e.getMessage(),
                    extractLineNumber(sql, e),
                    extractColumnNumber(sql, e),
                    e);
        }
    }

    /**
     * Extracts CREATE TABLE statements from SQL text.
     *
     * @param sql the SQL text containing one or more CREATE TABLE statements
     * @return a list of parsed CREATE TABLE statements
     * @throws SchemaParsingException if parsing fails
     */
    private List<SQLCreateTableStatement> extractCreateTableStatements(String sql)
            throws SchemaParsingException {
        List<SQLCreateTableStatement> statements = new ArrayList<>();

        // Split SQL by semicolons to handle multiple CREATE TABLE statements
        List<String> sqlParts = splitSqlBySemicolon(sql);

        for (String sqlPart : sqlParts) {
            String trimmedPart = sqlPart.trim();
            if (trimmedPart.isEmpty()) {
                continue;
            }

            // Check if this part contains a CREATE TABLE statement
            if (!trimmedPart.toUpperCase().contains("CREATE TABLE")) {
                throw new SchemaParsingException(
                        "Expected CREATE TABLE statement but found: "
                                + trimmedPart.substring(0, Math.min(50, trimmedPart.length())));
            }

            SQLCreateTableParser parser = createParser(trimmedPart);

            try {
                SQLCreateTableStatement statement = parser.parseCreateTable();
                if (statement == null) {
                    throw new SchemaParsingException("No CREATE TABLE statement found in SQL part");
                }
                statements.add(statement);
            } catch (Exception e) {
                throw new SchemaParsingException(
                        "Failed to parse CREATE TABLE statement: " + e.getMessage(), e);
            }
        }

        if (statements.isEmpty()) {
            throw new SchemaParsingException("No CREATE TABLE statement found in SQL");
        }

        return statements;
    }

    /**
     * Splits SQL text by semicolons, handling quoted strings properly.
     *
     * @param sql the SQL text
     * @return a list of SQL parts
     */
    private List<String> splitSqlBySemicolon(String sql) {
        List<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            // Track quotes to avoid splitting inside string literals
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }

            // Split on semicolons outside quotes
            if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                String part = currentPart.toString().trim();
                if (!part.isEmpty()) {
                    parts.add(part);
                }
                currentPart = new StringBuilder();
            } else {
                currentPart.append(c);
            }
        }

        // Add the last part
        String lastPart = currentPart.toString().trim();
        if (!lastPart.isEmpty()) {
            parts.add(lastPart);
        }

        return parts;
    }

    /**
     * Creates a database-specific CREATE TABLE parser.
     *
     * @param sql the SQL text to parse
     * @return a SQLCreateTableParser instance
     */
    private SQLCreateTableParser createParser(String sql) {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return new MySqlCreateTableParser(sql);
            case "postgresql":
                return new PGCreateTableParser(sql);
            case "oracle":
                return new OracleCreateTableParser(sql);
            default:
                return new SQLCreateTableParser(sql);
        }
    }

    /**
     * Extracts line number from exception if available.
     *
     * @param sql the original SQL
     * @param e the exception
     * @return the line number or -1 if not available
     */
    private int extractLineNumber(String sql, Exception e) {
        String message = e.getMessage();
        if (message != null && message.contains("line")) {
            try {
                String[] parts = message.split("line");
                if (parts.length > 1) {
                    String[] columnParts = parts[1].split(",");
                    return Integer.parseInt(columnParts[0].trim().split(" ")[1]);
                }
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    /**
     * Extracts column number from exception if available.
     *
     * @param sql the original SQL
     * @param e the exception
     * @return the column number or -1 if not available
     */
    private int extractColumnNumber(String sql, Exception e) {
        String message = e.getMessage();
        if (message != null && message.contains("column")) {
            try {
                String[] parts = message.split("column");
                if (parts.length > 1) {
                    return Integer.parseInt(parts[1].trim().split(" ")[1]);
                }
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    /**
     * Computes a SHA-256 hash of the SQL string for caching.
     *
     * @param sql the SQL string to hash
     * @return the hash string
     */
    private String computeHash(String sql) {
        synchronized (digest) {
            digest.reset();
            byte[] hash = digest.digest(sql.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
    }

    /**
     * Gets cache statistics.
     *
     * @return the cache statistics
     */
    public SchemaCache.CacheStatistics getCacheStatistics() {
        return cache.getStatistics();
    }

    /** Clears the schema cache. */
    public void clearCache() {
        cache.clear();
    }
}
