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
 * Generates MySQL-specific migration SQL statements.
 *
 * <p>This class converts SchemaDiff objects into MySQL-compatible ALTER TABLE and CREATE TABLE
 * statements that can transform a source schema into a target schema.
 */
public class MySQLMigrationGenerator implements MigrationGenerator {

    private final boolean includeComments;

    // Performance optimization: Template strings for common operations
    private static final String TEMPLATE_DROP_TABLE = "DROP TABLE %s;";
    private static final String TEMPLATE_CREATE_TABLE_HEADER = "CREATE TABLE %s (";
    private static final String TEMPLATE_ALTER_TABLE_HEADER = "ALTER TABLE %s";
    private static final String TEMPLATE_ADD_COLUMN = "ADD COLUMN %s";
    private static final String TEMPLATE_DROP_COLUMN = "DROP COLUMN %s";
    private static final String TEMPLATE_MODIFY_COLUMN = "MODIFY COLUMN %s";
    private static final String TEMPLATE_CREATE_INDEX = "CREATE %sINDEX %sON %s (%s)";
    private static final String TEMPLATE_DROP_INDEX = "DROP INDEX %s";

    // Pre-allocated StringBuilder for performance
    private static final int DEFAULT_STRING_BUILDER_SIZE = 1024;

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
        // Performance optimization: Pre-allocate with estimated capacity
        List<String> statements = new ArrayList<>(estimateCapacity(diff));

        // Check if there are any differences
        if (diff.isEmpty()) {
            return statements;
        }

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

    /**
     * Estimates the capacity needed for the statements list based on diff size. This helps avoid
     * repeated array resizing.
     */
    private int estimateCapacity(SchemaDiff diff) {
        int count = 0;
        // Count removed tables (1 statement each + blank line)
        count += diff.getRemovedTables().size() * 2;
        // Count added tables (1 statement each + blank line)
        count += diff.getAddedTables().size() * 2;
        // Count modified tables (estimate 3 statements each + blank lines)
        count += diff.getModifiedTables().size() * 4;
        // Add space for comments
        count += includeComments ? 4 : 0;
        return Math.max(count, 10); // Minimum capacity
    }

    /** Generates statements for removed tables. */
    private List<String> generateRemovedTables(SchemaDiff diff) {
        // Performance optimization: Pre-allocate with estimated capacity
        List<String> statements = new ArrayList<>(diff.getRemovedTables().size() * 2);

        for (Table table : diff.getRemovedTables().values()) {
            if (includeComments) {
                statements.add("-- Drop table: " + table.getName());
            }
            // Performance optimization: Use template with StringBuilder
            StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
            sb.append(String.format(TEMPLATE_DROP_TABLE, table.getName()));
            statements.add(sb.toString());
            statements.add("");
        }

        return statements;
    }

    /** Generates statements for added tables. */
    private List<String> generateAddedTables(SchemaDiff diff) {
        // Performance optimization: Pre-allocate with estimated capacity
        List<String> statements = new ArrayList<>(diff.getAddedTables().size() * 2);

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
        // Performance optimization: Pre-allocate with estimated capacity
        List<String> statements = new ArrayList<>(diff.getModifiedTables().size() * 4);

        for (TableDiff tableDiff : diff.getModifiedTables().values()) {
            statements.addAll(generateModifiedTable(tableDiff));
        }

        return statements;
    }

    /** Generates statements for a single modified table. */
    private List<String> generateModifiedTable(TableDiff tableDiff) {
        // Performance optimization: Pre-allocate with estimated capacity
        List<String> statements = new ArrayList<>(8);
        String tableName = tableDiff.getTableName();

        // Handle column and constraint changes first
        if (!tableDiff.getRemovedColumns().isEmpty()
                || !tableDiff.getModifiedColumns().isEmpty()
                || !tableDiff.getAddedColumns().isEmpty()
                || !tableDiff.getRemovedConstraints().isEmpty()
                || !tableDiff.getAddedConstraints().isEmpty()) {
            if (includeComments) {
                statements.add("-- Modify table: " + tableName);
            }

            // Performance optimization: Pre-allocate operations list
            List<String> operations =
                    new ArrayList<>(
                            tableDiff.getRemovedColumns().size()
                                    + tableDiff.getModifiedColumns().size()
                                    + tableDiff.getAddedColumns().size()
                                    + tableDiff.getRemovedConstraints().size()
                                    + tableDiff.getAddedConstraints().size());

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

            // Handle constraint changes
            operations.addAll(
                    tableDiff.getRemovedConstraints().stream()
                            .map(this::generateDropConstraint)
                            .collect(java.util.stream.Collectors.toList()));

            for (Constraint constraint : tableDiff.getAddedConstraints()) {
                operations.add(generateAddConstraint(constraint));
            }

            if (!operations.isEmpty()) {
                // Performance optimization: Use template with StringBuilder
                StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
                sb.append(String.format(TEMPLATE_ALTER_TABLE_HEADER, tableName));
                sb.append("\n    ");
                sb.append(String.join(",\n    ", operations));
                sb.append(";");
                statements.add(sb.toString());
            }
            statements.add("");
        }

        // Handle index changes separately (MySQL specific)
        if (!tableDiff.getRemovedIndexes().isEmpty()) {
            if (includeComments) {
                statements.add("-- Drop indexes from table: " + tableName);
            }
            for (String indexName : tableDiff.getRemovedIndexes()) {
                // Performance optimization: Use template with StringBuilder
                StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
                sb.append("ALTER TABLE ")
                        .append(tableName)
                        .append(" DROP INDEX ")
                        .append(indexName)
                        .append(";");
                statements.add(sb.toString());
            }
            statements.add("");
        }

        if (!tableDiff.getAddedIndexes().isEmpty()) {
            if (includeComments) {
                statements.add("-- Add indexes to table: " + tableName);
            }
            for (Index index : tableDiff.getAddedIndexes()) {
                statements.add(generateAddIndex(tableName, index));
            }
            statements.add("");
        }

        return statements;
    }

    /** Generates a CREATE TABLE statement. */
    private String generateCreateTable(Table table) {
        // Performance optimization: Pre-allocate StringBuilder
        StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
        sb.append(String.format(TEMPLATE_CREATE_TABLE_HEADER, table.getName()));
        sb.append("\n");

        // Performance optimization: Pre-allocate columnDefs list
        List<String> columnDefs =
                new ArrayList<>(table.getColumns().size() + table.getConstraints().size());
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
        // Performance optimization: Pre-allocate StringBuilder
        StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
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
        // Performance optimization: Use template with StringBuilder
        StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
        sb.append("ADD COLUMN ");
        sb.append(generateColumnDefinition(column));
        return sb.toString();
    }

    /** Generates a MODIFY COLUMN statement for a column change. */
    private String generateModifyColumn(ColumnDiff columnDiff) {
        Column oldColumn = columnDiff.getOldColumn();
        Column newColumn = columnDiff.getNewColumn();

        // Performance optimization: Pre-allocate StringBuilder
        StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
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

    /** Generates a DROP CONSTRAINT statement. */
    private String generateDropConstraint(String constraintName) {
        // In MySQL, PRIMARY KEY is dropped with ADD PRIMARY KEY syntax
        if (constraintName.equals("PRIMARY")) {
            return "DROP PRIMARY KEY";
        }
        // For other constraints, MySQL uses different syntax
        return "DROP INDEX " + constraintName;
    }

    /** Generates an ADD CONSTRAINT statement. */
    private String generateAddConstraint(Constraint constraint) {
        // Performance optimization: Use template with StringBuilder
        StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
        sb.append("ADD ");
        sb.append(generateConstraint(constraint));
        return sb.toString();
    }

    /** Generates a CREATE INDEX statement. */
    private String generateAddIndex(String tableName, Index index) {
        // Performance optimization: Pre-allocate StringBuilder
        StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
        sb.append("CREATE ");

        if (index.isUnique()) {
            sb.append("UNIQUE ");
        }

        sb.append("INDEX ");
        if (index.getName().isPresent()) {
            sb.append(index.getName().get()).append(" ");
        }
        sb.append("ON ").append(tableName);
        sb.append(" (").append(String.join(", ", index.getColumns())).append(")");

        if (index.getType().isPresent()) {
            sb.append(" USING ").append(index.getType().get());
        }

        sb.append(";");

        return sb.toString();
    }

    /** Escapes single quotes in comments. */
    private String escapeComment(String comment) {
        return comment.replace("'", "''");
    }
}
