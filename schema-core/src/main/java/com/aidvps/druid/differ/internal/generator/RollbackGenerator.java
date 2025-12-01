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

package com.aidvps.druid.differ.internal.generator;

import com.aidvps.druid.differ.internal.model.Column;
import com.aidvps.druid.differ.internal.model.ColumnDiff;
import com.aidvps.druid.differ.internal.model.Index;
import com.aidvps.druid.differ.internal.model.SchemaDiff;
import com.aidvps.druid.differ.internal.model.Table;
import com.aidvps.druid.differ.internal.model.TableDiff;
import com.aidvps.druid.differ.internal.model.constraint.Constraint;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates rollback SQL statements to reverse migration operations.
 *
 * <p>This class takes a SchemaDiff and generates the SQL statements needed to rollback the changes,
 * effectively transforming the target schema back to the source schema.
 */
public class RollbackGenerator {

    private final boolean includeComments;

    /**
     * Creates a new RollbackGenerator.
     *
     * @param includeComments whether to include comments in generated SQL
     */
    public RollbackGenerator(boolean includeComments) {
        this.includeComments = includeComments;
    }

    /**
     * Generates rollback statements from a schema diff.
     *
     * @param diff the schema diff to generate rollback for
     * @return a list of rollback SQL statements in reverse order
     */
    public List<String> generateRollback(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        if (diff.isEmpty()) {
            return statements;
        }

        if (includeComments) {
            statements.add("-- Rollback migration from target to source schema");
            statements.add("-- Generated: " + java.time.LocalDateTime.now());
            statements.add("");
        }

        // Generate rollback in reverse order
        // 1. Drop indexes (reverse of create index)
        statements.addAll(generateRollbackIndexes(diff));

        // 2. Drop constraints (reverse of add constraint)
        statements.addAll(generateRollbackConstraints(diff));

        // 3. Drop columns (reverse of add column)
        statements.addAll(generateRollbackColumns(diff));

        // 4. Drop tables (reverse of create table)
        statements.addAll(generateRollbackTables(diff));

        return statements;
    }

    /** Generates rollback statements for removed tables (reverse is create). */
    private List<String> generateRollbackTables(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (Table table : diff.getRemovedTables().values()) {
            if (includeComments) {
                statements.add("-- Recreate table: " + table.getName());
            }
            statements.add(generateCreateTable(table));
            statements.add("");
        }

        return statements;
    }

    /** Generates rollback statements for added tables (reverse is drop). */
    private List<String> generateRollbackAddedTables(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (Table table : diff.getAddedTables().values()) {
            if (includeComments) {
                statements.add("-- Drop table: " + table.getName());
            }
            statements.add("DROP TABLE " + table.getName() + ";");
            statements.add("");
        }

        return statements;
    }

    /** Generates rollback statements for modified tables. */
    private List<String> generateRollbackTableModifications(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (TableDiff tableDiff : diff.getModifiedTables().values()) {
            // Rollback column additions (drop them)
            for (Column column : tableDiff.getAddedColumns()) {
                if (includeComments) {
                    statements.add(
                            "-- Rollback column addition: "
                                    + tableDiff.getTableName()
                                    + "."
                                    + column.getName());
                }
                statements.add(
                        "ALTER TABLE "
                                + tableDiff.getTableName()
                                + " DROP COLUMN "
                                + column.getName()
                                + ";");
                statements.add("");
            }

            // Rollback column modifications (revert to old column)
            for (ColumnDiff columnDiff : tableDiff.getModifiedColumns().values()) {
                if (includeComments) {
                    statements.add(
                            "-- Rollback column modification: "
                                    + tableDiff.getTableName()
                                    + "."
                                    + columnDiff.getNewColumn().getName());
                }
                statements.add(
                        "ALTER TABLE "
                                + tableDiff.getTableName()
                                + " MODIFY COLUMN "
                                + generateColumnDefinition(columnDiff.getOldColumn())
                                + ";");
                statements.add("");
            }

            // Note: Rollback of removed columns is not possible without source schema metadata
        }

        return statements;
    }

    /** Generates rollback statements for columns. */
    private List<String> generateRollbackColumns(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        // Added columns are dropped in rollback
        for (TableDiff tableDiff : diff.getModifiedTables().values()) {
            for (Column column : tableDiff.getAddedColumns()) {
                if (includeComments) {
                    statements.add(
                            "-- Drop added column: "
                                    + tableDiff.getTableName()
                                    + "."
                                    + column.getName());
                }
                statements.add(
                        "ALTER TABLE "
                                + tableDiff.getTableName()
                                + " DROP COLUMN "
                                + column.getName()
                                + ";");
            }
        }

        return statements;
    }

    /** Generates rollback statements for constraints. */
    private List<String> generateRollbackConstraints(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (TableDiff tableDiff : diff.getModifiedTables().values()) {
            // Rollback added constraints (drop them)
            for (Constraint constraint : tableDiff.getAddedConstraints()) {
                if (includeComments) {
                    statements.add("-- Drop added constraint: " + constraint.getName());
                }
                statements.add(
                        "ALTER TABLE "
                                + tableDiff.getTableName()
                                + " DROP CONSTRAINT "
                                + constraint.getName()
                                + ";");
            }
        }

        return statements;
    }

    /** Generates rollback statements for indexes. */
    private List<String> generateRollbackIndexes(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (TableDiff tableDiff : diff.getModifiedTables().values()) {
            // Rollback added indexes (drop them)
            for (Index index : tableDiff.getAddedIndexes()) {
                if (includeComments) {
                    statements.add(
                            "-- Drop added index: "
                                    + tableDiff.getTableName()
                                    + "."
                                    + index.getName().orElse("unnamed"));
                }
                String indexName = index.getName().orElse("");
                statements.add("DROP INDEX " + indexName + " ON " + tableDiff.getTableName() + ";");
            }
        }

        return statements;
    }

    /** Generates a CREATE TABLE statement for rollback. */
    private String generateCreateTable(Table table) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(table.getName()).append(" (\n");

        List<String> columnDefs = new ArrayList<>();
        for (Column column : table.getColumns()) {
            columnDefs.add(generateColumnDefinition(column));
        }

        for (Constraint constraint : table.getConstraints().values()) {
            columnDefs.add(generateConstraint(constraint));
        }

        sb.append("    ").append(String.join(",\n    ", columnDefs));
        sb.append("\n);");

        table.getComment()
                .ifPresent(
                        comment -> {
                            sb.append("\nALTER TABLE ").append(table.getName());
                            sb.append(" COMMENT = '")
                                    .append(comment.replace("'", "''"))
                                    .append("';");
                        });

        return sb.toString();
    }

    /** Generates a column definition for rollback. */
    private String generateColumnDefinition(Column column) {
        StringBuilder sb = new StringBuilder();
        sb.append(column.getName()).append(" ").append(column.getDataType());

        column.getLength().ifPresent(length -> sb.append("(").append(length).append(")"));
        column.getPrecision()
                .ifPresent(
                        precision -> {
                            sb.append("(").append(precision);
                            column.getScale().ifPresent(scale -> sb.append(",").append(scale));
                            sb.append(")");
                        });

        if (!column.isNullable()) {
            sb.append(" NOT NULL");
        }

        if (column.isAutoIncrement()) {
            sb.append(" AUTO_INCREMENT");
        }

        column.getDefaultValue()
                .ifPresent(
                        defaultValue -> {
                            sb.append(" DEFAULT ").append(defaultValue);
                        });

        column.getComment()
                .ifPresent(
                        comment -> {
                            sb.append(" COMMENT '").append(comment.replace("'", "''")).append("'");
                        });

        return sb.toString();
    }

    /** Generates a constraint definition. */
    private String generateConstraint(Constraint constraint) {
        switch (constraint.getType()) {
            case PRIMARY_KEY:
                return "PRIMARY KEY ("
                        + String.join(
                                ", ",
                                ((com.aidvps.druid.differ.internal.model.constraint.PrimaryKey)
                                                constraint)
                                        .getColumns())
                        + ")";
            case UNIQUE:
                return "UNIQUE ("
                        + String.join(
                                ", ",
                                ((com.aidvps.druid.differ.internal.model.constraint
                                                        .UniqueConstraint)
                                                constraint)
                                        .getColumns())
                        + ")";
            case FOREIGN_KEY:
                return generateForeignKeyConstraint(
                        (com.aidvps.druid.differ.internal.model.constraint.ForeignKey) constraint);
            case CHECK:
                return "CHECK ("
                        + ((com.aidvps.druid.differ.internal.model.constraint.CheckConstraint)
                                        constraint)
                                .getExpression()
                        + ")";
            default:
                return "";
        }
    }

    /** Generates a foreign key constraint. */
    private String generateForeignKeyConstraint(
            com.aidvps.druid.differ.internal.model.constraint.ForeignKey fk) {
        StringBuilder sb = new StringBuilder();
        sb.append("FOREIGN KEY (").append(String.join(", ", fk.getColumns())).append(")");
        sb.append(" REFERENCES ").append(fk.getReferencedTable());
        sb.append(" (").append(String.join(", ", fk.getReferencedColumns())).append(")");

        fk.getOnDelete().ifPresent(action -> sb.append(" ON DELETE ").append(action));
        fk.getOnUpdate().ifPresent(action -> sb.append(" ON UPDATE ").append(action));

        return sb.toString();
    }
}
