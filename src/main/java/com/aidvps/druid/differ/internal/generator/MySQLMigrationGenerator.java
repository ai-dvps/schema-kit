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
import com.aidvps.druid.differ.internal.model.SchemaDiff;
import com.aidvps.druid.differ.internal.model.Table;
import com.aidvps.druid.differ.internal.model.TableDiff;
import com.aidvps.druid.differ.internal.model.constraint.Constraint;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates MySQL-specific migration SQL statements.
 *
 * <p>This class converts SchemaDiff objects into MySQL-compatible ALTER TABLE and CREATE TABLE
 * statements that can transform a source schema into a target schema.
 */
public class MySQLMigrationGenerator {

    private final boolean includeComments;

    /**
     * Creates a new MySQLMigrationGenerator.
     *
     * @param includeComments whether to include comments in generated SQL
     */
    public MySQLMigrationGenerator(boolean includeComments) {
        this.includeComments = includeComments;
    }

    /**
     * Generates migration SQL from a schema diff.
     *
     * @param diff the schema differences to convert
     * @return a list of SQL statements
     */
    public List<String> generate(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        if (includeComments) {
            statements.add("-- Migration from source to target schema");
            statements.add("-- Generated: " + java.time.LocalDateTime.now());
            statements.add("-- Database: MySQL");
            statements.add("");
        }

        statements.addAll(generateRemovedTables(diff));
        statements.addAll(generateModifiedTables(diff));
        statements.addAll(generateAddedTables(diff));

        return statements;
    }

    /** Generates statements for removed tables. */
    private List<String> generateRemovedTables(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (Table table : diff.getRemovedTables().values()) {
            if (includeComments) {
                statements.add("-- Drop table: " + table.getName());
            }
            statements.add("DROP TABLE " + table.getName() + ";");
            statements.add("");
        }

        return statements;
    }

    /** Generates statements for added tables. */
    private List<String> generateAddedTables(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (Table table : diff.getAddedTables().values()) {
            if (includeComments) {
                statements.add("-- Create table: " + table.getName());
            }
            statements.add(generateCreateTable(table));
            statements.add("");
        }

        return statements;
    }

    /** Generates statements for modified tables. */
    private List<String> generateModifiedTables(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (TableDiff tableDiff : diff.getModifiedTables().values()) {
            statements.addAll(generateModifiedTable(tableDiff));
        }

        return statements;
    }

    /** Generates statements for a single modified table. */
    private List<String> generateModifiedTable(TableDiff tableDiff) {
        List<String> statements = new ArrayList<>();
        String tableName = tableDiff.getTableName();

        if (!tableDiff.getRemovedColumns().isEmpty()
                || !tableDiff.getModifiedColumns().isEmpty()
                || !tableDiff.getAddedColumns().isEmpty()) {
            if (includeComments) {
                statements.add("-- Modify table: " + tableName);
            }

            List<String> operations = new ArrayList<>();

            operations.addAll(
                    tableDiff.getRemovedColumns().stream()
                            .map(col -> "DROP COLUMN " + col)
                            .collect(java.util.stream.Collectors.toList()));

            for (ColumnDiff columnDiff : tableDiff.getModifiedColumns().values()) {
                operations.add(generateModifyColumn(columnDiff));
            }

            operations.addAll(
                    tableDiff.getAddedColumns().stream()
                            .map(this::generateAddColumn)
                            .collect(java.util.stream.Collectors.toList()));

            if (!operations.isEmpty()) {
                statements.add("ALTER TABLE " + tableName);
                statements.add("    " + String.join(",\n    ", operations) + ";");
            }
            statements.add("");
        }

        return statements;
    }

    /** Generates a CREATE TABLE statement. */
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
                            sb.append(" COMMENT = '").append(escapeComment(comment)).append("';");
                        });

        return sb.toString();
    }

    /** Generates a column definition. */
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
                            sb.append(" COMMENT '").append(escapeComment(comment)).append("'");
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

    /** Generates an ADD COLUMN statement. */
    private String generateAddColumn(Column column) {
        return "ADD COLUMN " + generateColumnDefinition(column);
    }

    /** Generates a MODIFY COLUMN statement for a column change. */
    private String generateModifyColumn(ColumnDiff columnDiff) {
        Column oldColumn = columnDiff.getOldColumn();
        Column newColumn = columnDiff.getNewColumn();

        StringBuilder sb = new StringBuilder();
        sb.append("MODIFY COLUMN ").append(newColumn.getName()).append(" ");

        sb.append(newColumn.getDataType());
        newColumn.getLength().ifPresent(length -> sb.append("(").append(length).append(")"));
        newColumn
                .getPrecision()
                .ifPresent(
                        precision -> {
                            sb.append("(").append(precision);
                            newColumn.getScale().ifPresent(scale -> sb.append(",").append(scale));
                            sb.append(")");
                        });

        if (!newColumn.isNullable()) {
            sb.append(" NOT NULL");
        } else {
            sb.append(" NULL");
        }

        if (newColumn.isAutoIncrement()) {
            sb.append(" AUTO_INCREMENT");
        }

        newColumn
                .getDefaultValue()
                .ifPresent(
                        defaultValue -> {
                            sb.append(" DEFAULT ").append(defaultValue);
                        });

        newColumn
                .getComment()
                .ifPresent(
                        comment -> {
                            sb.append(" COMMENT '").append(escapeComment(comment)).append("'");
                        });

        return sb.toString();
    }

    /** Escapes single quotes in comments. */
    private String escapeComment(String comment) {
        return comment.replace("'", "''");
    }
}
