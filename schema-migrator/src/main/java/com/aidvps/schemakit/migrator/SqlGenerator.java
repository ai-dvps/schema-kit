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

package com.aidvps.schemakit.migrator;

import java.util.ArrayList;
import java.util.List;

/** Generates SQL statements from schema diffs. */
public class SqlGenerator {
    /**
     * Generate SQL statements from a schema diff.
     *
     * @param diff The schema diff
     * @param config Migration configuration
     * @return List of SQL statements
     */
    public List<MigrationStatement> generate(SchemaDiff diff, MigrationConfig config) {
        List<MigrationStatement> statements = new ArrayList<>();

        if (diff == null || !diff.hasChanges()) {
            return statements;
        }

        // Get target platform for dialect-specific SQL generation
        String platform = config.getTargetPlatform().name();

        // Generate SQL for each change
        for (SchemaChange change : diff.getChanges()) {
            statements.add(generateSqlForChange(change, config));
        }

        // Wrap in transaction if configured
        if (config.isTransactional()) {
            statements = wrapInTransaction(statements, platform);
        }

        return statements;
    }

    /**
     * Generate SQL for a single schema change.
     *
     * @param change The schema change
     * @param config Migration configuration
     * @return MigrationStatement with SQL
     */
    private MigrationStatement generateSqlForChange(SchemaChange change, MigrationConfig config) {
        String sql = "";
        String description = change.getDescription();

        switch (change.getType()) {
            case DATABASE_CREATED:
                sql = generateCreateDatabaseSql(change, config);
                break;
            case DATABASE_DROPPED:
                if (config.isIncludeDrops()) {
                    sql = generateDropDatabaseSql(change, config);
                } else {
                    sql = "-- DROP DATABASE skipped by config";
                }
                break;
            case TABLE_CREATED:
                sql = generateCreateTableSql(change, config);
                break;
            case TABLE_DROPPED:
                if (config.isIncludeDrops()) {
                    sql = generateDropTableSql(change, config);
                } else {
                    sql = "-- DROP TABLE skipped by config";
                }
                break;
            case COLUMN_ADDED:
                sql = generateAddColumnSql(change, config);
                break;
            case COLUMN_DROPPED:
                if (config.isIncludeDrops()) {
                    sql = generateDropColumnSql(change, config);
                } else {
                    sql = "-- DROP COLUMN skipped by config";
                }
                break;
            case INDEX_CREATED:
                sql = generateCreateIndexSql(change, config);
                break;
            case INDEX_DROPPED:
                if (config.isIncludeDrops()) {
                    sql = generateDropIndexSql(change, config);
                } else {
                    sql = "-- DROP INDEX skipped by config";
                }
                break;
            case CONSTRAINT_ADDED:
                sql = generateAddConstraintSql(change, config);
                break;
            case CONSTRAINT_DROPPED:
                if (config.isIncludeDrops()) {
                    sql = generateDropConstraintSql(change, config);
                } else {
                    sql = "-- DROP CONSTRAINT skipped by config";
                }
                break;
            default:
                sql = "-- Unhandled change type: " + change.getType();
                break;
        }

        return MigrationStatement.builder().sql(sql).description(description).build();
    }

    private String generateCreateDatabaseSql(SchemaChange change, MigrationConfig config) {
        String dbName = extractEntityName(change.getEntityPath());
        String sql = "CREATE DATABASE " + quoteIdentifier(dbName, config);
        return sql + ";";
    }

    private String generateDropDatabaseSql(SchemaChange change, MigrationConfig config) {
        String dbName = extractEntityName(change.getEntityPath());
        String sql = "DROP DATABASE " + quoteIdentifier(dbName, config);
        return sql + ";";
    }

    private String generateCreateTableSql(SchemaChange change, MigrationConfig config) {
        // Placeholder - in real implementation would generate CREATE TABLE with columns
        String tableName = change.getEntityPath();
        String sql = "-- CREATE TABLE " + tableName;
        sql += "\nCREATE TABLE " + quoteIdentifier(tableName, config) + " (";
        sql += "\n  -- Column definitions would go here";
        sql += "\n);";
        return sql;
    }

    private String generateDropTableSql(SchemaChange change, MigrationConfig config) {
        String tableName = change.getEntityPath();
        return "DROP TABLE " + quoteIdentifier(tableName, config) + ";";
    }

    private String generateAddColumnSql(SchemaChange change, MigrationConfig config) {
        String tableName = extractTableName(change.getEntityPath());
        String columnName = extractColumnName(change.getEntityPath());
        String sql = "ALTER TABLE " + quoteIdentifier(tableName, config);
        sql += " ADD COLUMN " + quoteIdentifier(columnName, config) + " VARCHAR(255);";
        return sql;
    }

    private String generateDropColumnSql(SchemaChange change, MigrationConfig config) {
        String tableName = extractTableName(change.getEntityPath());
        String columnName = extractColumnName(change.getEntityPath());
        return "ALTER TABLE "
                + quoteIdentifier(tableName, config)
                + " DROP COLUMN "
                + quoteIdentifier(columnName, config)
                + ";";
    }

    private String generateCreateIndexSql(SchemaChange change, MigrationConfig config) {
        String tableName = change.getEntityPath();
        String indexName = "idx_" + tableName.replace(".", "_");
        return "CREATE INDEX "
                + quoteIdentifier(indexName, config)
                + " ON "
                + quoteIdentifier(tableName, config)
                + ";";
    }

    private String generateDropIndexSql(SchemaChange change, MigrationConfig config) {
        String tableName = change.getEntityPath();
        String indexName = "idx_" + tableName.replace(".", "_");
        return "DROP INDEX " + quoteIdentifier(indexName, config) + ";";
    }

    private String generateAddConstraintSql(SchemaChange change, MigrationConfig config) {
        return "-- ADD CONSTRAINT " + change.getEntityPath();
    }

    private String generateDropConstraintSql(SchemaChange change, MigrationConfig config) {
        return "-- DROP CONSTRAINT " + change.getEntityPath();
    }

    private String quoteIdentifier(String identifier, MigrationConfig config) {
        switch (config.getTargetPlatform()) {
            case MYSQL:
            case MARIADB:
                return "`" + identifier.replace(".", "`.`") + "`";
            case POSTGRESQL:
            case SQLITE:
                return "\"" + identifier.replace(".", "\".\"") + "\"";
            default:
                return identifier;
        }
    }

    private String extractEntityName(String entityPath) {
        // Extract database name from "db.table" format
        return entityPath.contains(".") ? entityPath.split("\\.")[0] : entityPath;
    }

    private String extractTableName(String entityPath) {
        // Extract table name from "db.table" format
        return entityPath.contains(".")
                ? entityPath.substring(entityPath.indexOf(".") + 1)
                : entityPath;
    }

    private String extractColumnName(String entityPath) {
        // Extract column name from "db.table.column" format
        return entityPath.contains(".") && entityPath.split("\\.").length > 2
                ? entityPath.substring(entityPath.lastIndexOf(".") + 1)
                : entityPath;
    }

    private List<MigrationStatement> wrapInTransaction(
            List<MigrationStatement> statements, String platform) {
        List<MigrationStatement> wrapped = new ArrayList<>();

        // Add start transaction
        switch (platform) {
            case "MYSQL":
            case "MARIADB":
            case "SQLITE":
                wrapped.add(
                        MigrationStatement.builder()
                                .sql("START TRANSACTION;")
                                .description("Start transaction")
                                .build());
                break;
            case "POSTGRESQL":
                wrapped.add(
                        MigrationStatement.builder()
                                .sql("BEGIN;")
                                .description("Begin transaction")
                                .build());
                break;
            default:
                break;
        }

        // Add all statements
        wrapped.addAll(statements);

        // Add commit
        wrapped.add(
                MigrationStatement.builder()
                        .sql("COMMIT;")
                        .description("Commit transaction")
                        .build());

        return wrapped;
    }
}
