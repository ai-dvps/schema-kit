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
 * Generates Oracle-specific migration SQL statements.
 *
 * <p>This class converts SchemaDiff objects into Oracle-compatible ALTER TABLE and CREATE TABLE
 * statements that can transform a source schema into a target schema.
 *
 * <p>Oracle-specific features:
 *
 * <ul>
 *   <li>Uses VARCHAR2 instead of VARCHAR
 *   <li>Uses NUMBER instead of INT/BIGINT
 *   <li>Requires parentheses for multi-column ALTER TABLE operations
 *   <li>Uses CLOB for large text fields
 *   <li>Requires constraint names for all DROP operations
 * </ul>
 */
public class OracleMigrationGenerator implements MigrationGenerator {

    private final boolean includeComments;

    // Oracle-specific template strings
    private static final String TEMPLATE_DROP_TABLE = "DROP TABLE %s;";
    private static final String TEMPLATE_CREATE_TABLE_HEADER = "CREATE TABLE %s (";
    private static final String TEMPLATE_ALTER_TABLE_HEADER = "ALTER TABLE %s";
    private static final String TEMPLATE_ADD_COLUMN = "ADD ( %s )";
    private static final String TEMPLATE_DROP_COLUMN = "DROP ( %s )";
    private static final String TEMPLATE_MODIFY_COLUMN = "MODIFY ( %s )";
    private static final String TEMPLATE_CREATE_INDEX = "CREATE %sINDEX %sON %s (%s)";
    private static final String TEMPLATE_DROP_INDEX = "DROP INDEX %s";
    private static final String TEMPLATE_ADD_CONSTRAINT = "ADD CONSTRAINT %s %s";
    private static final String TEMPLATE_DROP_CONSTRAINT = "DROP CONSTRAINT %s";

    // Pre-allocated StringBuilder for performance
    private static final int DEFAULT_STRING_BUILDER_SIZE = 1024;

    /**
     * Creates a new OracleMigrationGenerator.
     *
     * @param includeComments whether to include comments in generated SQL
     */
    public OracleMigrationGenerator(boolean includeComments) {
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
            statements.add("-- Database: Oracle");
            statements.add("");
        }

        statements.addAll(generateRemovedTables(diff));
        statements.addAll(generateModifiedTables(diff));
        statements.addAll(generateAddedTables(diff));

        return statements;
    }

    /** Estimates the capacity needed for the statements list based on diff size. */
    private int estimateCapacity(SchemaDiff diff) {
        int count = 0;
        count += diff.getRemovedTables().size() * 2;
        count += diff.getAddedTables().size() * 2;
        count += diff.getModifiedTables().size() * 5;
        return count;
    }

    /** Generates SQL for removed tables. */
    private List<String> generateRemovedTables(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (Table table : diff.getRemovedTables().values()) {
            if (includeComments) {
                statements.add("-- Drop table: " + table.getName());
            }
            statements.add(String.format(TEMPLATE_DROP_TABLE, table.getName()));
            statements.add("");
        }

        return statements;
    }

    /** Generates SQL for added tables. */
    private List<String> generateAddedTables(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (Table table : diff.getAddedTables().values()) {
            if (includeComments) {
                statements.add("-- Create table: " + table.getName());
            }

            StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
            sb.append(String.format(TEMPLATE_CREATE_TABLE_HEADER, table.getName()));
            sb.append("\n");

            List<String> columnDefs = new ArrayList<>();
            for (Column column : table.getColumns()) {
                columnDefs.add(generateColumnDefinition(column));
            }

            for (Constraint constraint : table.getConstraints().values()) {
                columnDefs.add(generateConstraint(constraint));
            }

            sb.append("    ").append(String.join(",\n    ", columnDefs));
            sb.append("\n);");

            statements.add(sb.toString());
            statements.add("");
        }

        return statements;
    }

    /** Generates SQL for modified tables. */
    private List<String> generateModifiedTables(SchemaDiff diff) {
        List<String> statements = new ArrayList<>();

        for (TableDiff tableDiff : diff.getModifiedTables().values()) {
            String tableName = tableDiff.getTableName();

            if (includeComments) {
                statements.add("-- Modify table: " + tableName);
            }

            // Oracle groups ALTER TABLE operations
            statements.addAll(generateColumnChanges(tableName, tableDiff));
            statements.addAll(generateConstraintChanges(tableName, tableDiff));
            statements.addAll(generateIndexChanges(tableName, tableDiff));

            statements.add("");
        }

        return statements;
    }

    /** Generates SQL for column changes. */
    private List<String> generateColumnChanges(String tableName, TableDiff tableDiff) {
        List<String> statements = new ArrayList<>();

        // Oracle requires grouped operations for ALTER TABLE
        List<String> operations = new ArrayList<>();

        // Drop removed columns
        for (String columnName : tableDiff.getRemovedColumns()) {
            operations.add(String.format(TEMPLATE_DROP_COLUMN, columnName));
        }

        // Add added columns
        for (Column column : tableDiff.getAddedColumns()) {
            operations.add(String.format(TEMPLATE_ADD_COLUMN, generateColumnDefinition(column)));
        }

        // Modify changed columns
        for (ColumnDiff columnDiff : tableDiff.getModifiedColumns().values()) {
            operations.addAll(generateColumnModification(columnDiff));
        }

        // Generate single ALTER TABLE statement if there are operations
        if (!operations.isEmpty()) {
            if (includeComments) {
                statements.add("-- Modify columns");
            }
            statements.add(
                    String.format(
                            TEMPLATE_ALTER_TABLE_HEADER + " " + String.join(" ", operations) + ";",
                            tableName));
        }

        return statements;
    }

    /** Generates SQL for a single column modification. */
    private List<String> generateColumnModification(ColumnDiff columnDiff) {
        List<String> operations = new ArrayList<>();
        Column oldColumn = columnDiff.getOldColumn();
        Column newColumn = columnDiff.getNewColumn();

        StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
        sb.append(newColumn.getName()).append(" ").append(generateDataType(newColumn));

        // Oracle MODIFY includes type and constraints
        if (!newColumn.isNullable()) {
            sb.append(" NOT NULL");
        }

        if (newColumn.isAutoIncrement()) {
            // Oracle uses sequences for auto-increment
            sb.append(" GENERATED ALWAYS AS IDENTITY");
        }

        if (newColumn.getDefaultValue().isPresent()) {
            sb.append(" DEFAULT ").append(newColumn.getDefaultValue().get());
        }

        operations.add(String.format(TEMPLATE_MODIFY_COLUMN, sb.toString()));

        return operations;
    }

    /** Generates constraint changes. */
    private List<String> generateConstraintChanges(String tableName, TableDiff tableDiff) {
        List<String> statements = new ArrayList<>();

        // Drop removed constraints
        for (String constraintName : tableDiff.getRemovedConstraints()) {
            if (includeComments) {
                statements.add("-- Drop constraint: " + constraintName);
            }
            statements.add(
                    String.format(
                            TEMPLATE_ALTER_TABLE_HEADER + " " + TEMPLATE_DROP_CONSTRAINT + ";",
                            tableName,
                            constraintName));
        }

        // Add added constraints
        for (Constraint constraint : tableDiff.getAddedConstraints()) {
            if (includeComments) {
                statements.add("-- Add constraint: " + constraint.getName().orElse("unnamed"));
            }

            statements.add(
                    String.format(
                            TEMPLATE_ALTER_TABLE_HEADER + " " + TEMPLATE_ADD_CONSTRAINT + ";",
                            tableName,
                            constraint.getName().orElse(generateConstraintName(constraint)),
                            generateConstraint(constraint)));
        }

        return statements;
    }

    /** Generates index changes. */
    private List<String> generateIndexChanges(String tableName, TableDiff tableDiff) {
        List<String> statements = new ArrayList<>();

        // Drop removed indexes
        for (String indexName : tableDiff.getRemovedIndexes()) {
            if (includeComments) {
                statements.add("-- Drop index: " + indexName);
            }
            statements.add(String.format(TEMPLATE_DROP_INDEX, indexName) + ";");
        }

        // Add added indexes
        for (Index index : tableDiff.getAddedIndexes()) {
            if (includeComments) {
                statements.add("-- Add index: " + index.getName().orElse("unnamed"));
            }

            statements.add(
                    String.format(
                                    TEMPLATE_CREATE_INDEX,
                                    index.isUnique() ? "UNIQUE " : "",
                                    index.getName().orElse(""),
                                    tableName,
                                    String.join(", ", index.getColumns()))
                            + ";");
        }

        return statements;
    }

    /** Generates a column definition for Oracle. */
    private String generateColumnDefinition(Column column) {
        StringBuilder sb = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
        sb.append(column.getName()).append(" ").append(generateDataType(column));

        if (!column.isNullable()) {
            sb.append(" NOT NULL");
        }

        if (column.isAutoIncrement()) {
            // Oracle prefers IDENTITY
            sb.append(" GENERATED ALWAYS AS IDENTITY");
        }

        column.getDefaultValue()
                .ifPresent(
                        defaultValue -> {
                            sb.append(" DEFAULT ").append(defaultValue);
                        });

        column.getComment()
                .ifPresent(
                        comment -> {
                            if (includeComments) {
                                sb.append(" -- ").append(comment.replace("\n", " "));
                            }
                        });

        return sb.toString();
    }

    /** Generates Oracle-specific data type. */
    private String generateDataType(Column column) {
        StringBuilder sb = new StringBuilder();

        // Handle Oracle-specific types
        String dataType = column.getDataType().toUpperCase();

        // Convert generic types to Oracle equivalents
        if (dataType.equals("INT") || dataType.equals("INTEGER")) {
            sb.append("NUMBER(10)");
        } else if (dataType.equals("BIGINT")) {
            sb.append("NUMBER(19)");
        } else if (dataType.equals("VARCHAR") || dataType.equals("VARCHAR2")) {
            sb.append("VARCHAR2");
            if (column.getLength().isPresent()) {
                sb.append("(").append(column.getLength().get()).append(")");
            }
        } else if (dataType.equals("TEXT") || dataType.equals("CLOB")) {
            sb.append("CLOB");
        } else if (dataType.equals("TIMESTAMP")) {
            sb.append("TIMESTAMP");
        } else if (dataType.equals("BOOLEAN")) {
            sb.append("NUMBER(1)");
        } else if (dataType.equals("UUID")) {
            sb.append("VARCHAR2(36)");
        } else if (dataType.equals("JSONB")) {
            sb.append("CLOB");
        } else {
            // For other types, use as-is
            sb.append(column.getDataType());
            if (column.getLength().isPresent()) {
                sb.append("(").append(column.getLength().get()).append(")");
            }
            if (column.getPrecision().isPresent()) {
                sb.append("(").append(column.getPrecision().get());
                if (column.getScale().isPresent()) {
                    sb.append(",").append(column.getScale().get());
                }
                sb.append(")");
            }
        }

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
        sb.append("FOREIGN KEY (")
                .append(String.join(", ", fk.getColumns()))
                .append(") REFERENCES ")
                .append(fk.getReferencedTable())
                .append(" (")
                .append(String.join(", ", fk.getReferencedColumns()))
                .append(")");

        fk.getOnDelete().ifPresent(action -> sb.append(" ON DELETE ").append(action));
        fk.getOnUpdate().ifPresent(action -> sb.append(" ON UPDATE ").append(action));

        return sb.toString();
    }

    /** Generates a constraint name if not present. */
    private String generateConstraintName(Constraint constraint) {
        return constraint.getType().name().toLowerCase() + "_" + System.currentTimeMillis();
    }
}
