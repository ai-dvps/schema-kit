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
import com.aidvps.druid.differ.internal.model.Column;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.Table;
import com.aidvps.druid.differ.internal.model.constraint.Constraint;
import com.aidvps.druid.differ.internal.model.constraint.ForeignKey;
import com.aidvps.druid.differ.internal.model.constraint.PrimaryKey;
import com.aidvps.druid.differ.internal.model.constraint.UniqueConstraint;
import com.aidvps.druid.sql.ast.SQLExpr;
import com.aidvps.druid.sql.ast.expr.SQLIdentifierExpr;
import com.aidvps.druid.sql.ast.expr.SQLIntegerExpr;
import com.aidvps.druid.sql.ast.expr.SQLNullExpr;
import com.aidvps.druid.sql.ast.expr.SQLPropertyExpr;
import com.aidvps.druid.sql.ast.expr.SQLTextLiteralExpr;
import com.aidvps.druid.sql.ast.statement.SQLColumnConstraint;
import com.aidvps.druid.sql.ast.statement.SQLColumnDefinition;
import com.aidvps.druid.sql.ast.statement.SQLColumnPrimaryKey;
import com.aidvps.druid.sql.ast.statement.SQLConstraint;
import com.aidvps.druid.sql.ast.statement.SQLCreateTableStatement;
import com.aidvps.druid.sql.ast.statement.SQLForeignKeyConstraint;
import com.aidvps.druid.sql.ast.statement.SQLNotNullConstraint;
import com.aidvps.druid.sql.ast.statement.SQLPrimaryKey;
import com.aidvps.druid.sql.ast.statement.SQLTableElement;
import com.aidvps.druid.sql.ast.statement.SQLUnique;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Extracts schema information from druid-parser AST objects.
 *
 * <p>This class converts druid-parser's AST representations (SQLCreateTableStatement, etc.) into
 * our internal Schema model objects.
 */
class SchemaExtractor {

    /**
     * Extracts a Schema from a list of CREATE TABLE statements.
     *
     * @param statements the list of CREATE TABLE statements
     * @param dbType the database type
     * @return a Schema object
     */
    Schema extract(List<SQLCreateTableStatement> statements, String dbType) {
        DatabaseDialect dialect = determineDialect(dbType);
        Schema.Builder schemaBuilder = Schema.builder(dialect);

        for (SQLCreateTableStatement statement : statements) {
            Table table = extractTable(statement);
            schemaBuilder.addTable(table);
        }

        return schemaBuilder.build();
    }

    /**
     * Extracts a Table from a CREATE TABLE statement.
     *
     * @param statement the CREATE TABLE statement
     * @return a Table object
     */
    private Table extractTable(SQLCreateTableStatement statement) {
        String tableName = extractTableName(statement.getName());
        Table.Builder tableBuilder = Table.builder(tableName);

        List<SQLTableElement> elements = statement.getTableElementList();
        if (elements == null || elements.isEmpty()) {
            throw new IllegalArgumentException("CREATE TABLE statement has no table elements");
        }

        for (SQLTableElement element : elements) {
            if (element instanceof SQLColumnDefinition) {
                SQLColumnDefinition columnDef = (SQLColumnDefinition) element;
                Column column = extractColumn(columnDef);
                tableBuilder.addColumn(column);

                // Extract any inline constraints (e.g., PRIMARY KEY) from the column
                List<Constraint> inlineConstraints = extractInlineColumnConstraints(columnDef, tableName);
                for (Constraint constraint : inlineConstraints) {
                    String constraintName = constraint.getName().orElse("PRIMARY");
                    tableBuilder.addConstraint(constraintName, constraint);
                }
            } else if (element instanceof SQLConstraint) {
                Constraint constraint = extractConstraint((SQLConstraint) element, tableName);
                String constraintName = getConstraintName((SQLConstraint) element);
                tableBuilder.addConstraint(constraintName, constraint);
            }
        }

        String comment = extractTableComment(statement);
        if (comment != null) {
            tableBuilder.comment(comment);
        }

        return tableBuilder.build();
    }

    /**
     * Extracts a Column from a SQLColumnDefinition.
     *
     * @param columnDef the column definition
     * @return a Column object
     */
    private Column extractColumn(SQLColumnDefinition columnDef) {
        String columnName = columnDef.getColumnName();

        Column.Builder builder =
                Column.builder(columnName, extractDataType(columnDef.getDataType()));

        Integer length = extractLength(columnDef);
        if (length != null) {
            builder.length(length);
        }

        Integer precision = extractPrecision(columnDef);
        Integer scale = extractScale(columnDef);
        if (precision != null && scale != null) {
            builder.precision(precision, scale);
        }

        boolean nullable = !hasNotNullConstraint(columnDef);
        builder.nullable(nullable);

        String defaultValue = extractDefaultValue(columnDef);
        if (defaultValue != null) {
            builder.defaultValue(defaultValue);
        }

        boolean autoIncrement = columnDef.isAutoIncrement();
        if (autoIncrement) {
            builder.autoIncrement(true);
        }

        String comment = extractColumnComment(columnDef);
        if (comment != null) {
            builder.comment(comment);
        }

        String characterSet = extractCharacterSet(columnDef);
        if (characterSet != null) {
            builder.characterSet(characterSet);
        }

        String collation = extractCollation(columnDef);
        if (collation != null) {
            builder.collation(collation);
        }

        return builder.build();
    }

    /**
     * Extracts inline constraints from a column definition.
     *
     * @param columnDef the column definition
     * @param tableName the table name (for generating constraint names if needed)
     * @return a list of extracted constraints (may be empty)
     */
    private List<Constraint> extractInlineColumnConstraints(SQLColumnDefinition columnDef, String tableName) {
        List<Constraint> constraints = new ArrayList<>();
        List<SQLColumnConstraint> columnConstraints = columnDef.getConstraints();

        if (columnConstraints == null) {
            return constraints;
        }

        for (SQLColumnConstraint constraint : columnConstraints) {
            if (constraint instanceof SQLColumnPrimaryKey) {
                // Extract PRIMARY KEY constraint from column
                // SQLColumnPrimaryKey is a marker, so we use the column name directly
                PrimaryKey primaryKey = extractColumnPrimaryKey(columnDef);
                constraints.add(primaryKey);
            }
        }

        return constraints;
    }

    /**
     * Extracts a PrimaryKey constraint from a column definition.
     *
     * @param columnDef the column definition that has a PRIMARY KEY constraint
     * @return a PrimaryKey object
     */
    private PrimaryKey extractColumnPrimaryKey(SQLColumnDefinition columnDef) {
        List<String> columns = new ArrayList<>();
        // For inline PRIMARY KEY, the column name is in the column definition itself
        columns.add(columnDef.getColumnName());

        return new PrimaryKey(null, columns);
    }

    /**
     * Extracts a Constraint from a SQLConstraint.
     *
     * @param constraint the SQL constraint
     * @param tableName the table name (for context)
     * @return a Constraint object
     */
    private Constraint extractConstraint(SQLConstraint constraint, String tableName) {
        if (constraint instanceof SQLPrimaryKey) {
            return extractPrimaryKey((SQLPrimaryKey) constraint);
        } else if (constraint instanceof SQLForeignKeyConstraint) {
            return extractForeignKey((SQLForeignKeyConstraint) constraint, tableName);
        } else if (constraint instanceof SQLUnique) {
            return extractUniqueConstraint((SQLUnique) constraint);
        } else if (constraint instanceof SQLNotNullConstraint) {
            return null; // NOT NULL is handled at column level
        }
        return null;
    }

    /**
     * Extracts a PrimaryKey constraint.
     *
     * @param pk the primary key constraint
     * @return a PrimaryKey object
     */
    private PrimaryKey extractPrimaryKey(SQLPrimaryKey pk) {
        List<String> columns = new ArrayList<>();
        for (SQLExpr expr : pk.getColumns()) {
            columns.add(extractIdentifier(expr));
        }

        String name = pk.getName() != null ? extractIdentifier(pk.getName()) : null;
        return new PrimaryKey(name, columns);
    }

    /**
     * Extracts a ForeignKey constraint.
     *
     * @param fk the foreign key constraint
     * @param tableName the table name
     * @return a ForeignKey object
     */
    private ForeignKey extractForeignKey(SQLForeignKeyConstraint fk, String tableName) {
        List<String> columns = new ArrayList<>();
        for (com.aidvps.druid.sql.ast.SQLName name : fk.getReferencingColumns()) {
            columns.add(name.getSimpleName());
        }

        List<String> referencedColumns = new ArrayList<>();
        for (com.aidvps.druid.sql.ast.SQLName name : fk.getReferencedColumns()) {
            referencedColumns.add(name.getSimpleName());
        }

        String referencedTable =
                fk.getReferencedTableName() != null
                        ? fk.getReferencedTableName().getSimpleName()
                        : null;

        ForeignKey.OnAction onDelete = null;
        ForeignKey.OnAction onUpdate = null;

        String name = fk.getName() != null ? fk.getName().getSimpleName() : null;
        return new ForeignKey(
                name, columns, referencedTable, referencedColumns, onDelete, onUpdate, true, false);
    }

    /**
     * Extracts a Unique constraint.
     *
     * @param unique the unique constraint
     * @return a UniqueConstraint object
     */
    private UniqueConstraint extractUniqueConstraint(SQLUnique unique) {
        List<String> columns = new ArrayList<>();
        for (SQLExpr expr : unique.getColumns()) {
            columns.add(extractIdentifier(expr));
        }

        String name = unique.getName() != null ? extractIdentifier(unique.getName()) : null;
        return new UniqueConstraint(name, columns);
    }

    /** Extracts ON DELETE/UPDATE action. */
    private ForeignKey.OnAction extractOnAction(SQLExpr expr) {
        if (expr == null) {
            return null;
        }
        String action = expr.toString().toUpperCase(Locale.ROOT);
        try {
            return ForeignKey.OnAction.valueOf(action);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Extracts table name from SQLName. */
    private String extractTableName(com.aidvps.druid.sql.ast.SQLName name) {
        if (name == null) {
            return null;
        }
        return name.getSimpleName();
    }

    /** Extracts identifier from SQLExpr. */
    private String extractIdentifier(SQLExpr expr) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) expr).getName();
        } else if (expr instanceof SQLPropertyExpr) {
            return ((SQLPropertyExpr) expr).getName();
        }
        return expr.toString();
    }

    /** Extracts data type string. */
    private String extractDataType(com.aidvps.druid.sql.ast.SQLDataType dataType) {
        if (dataType == null) {
            return "VARCHAR";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(dataType.getName());
        if (dataType.getArguments() != null && !dataType.getArguments().isEmpty()) {
            sb.append('(');
            boolean first = true;
            for (SQLExpr arg : dataType.getArguments()) {
                if (!first) {
                    sb.append(',');
                }
                if (arg instanceof SQLIntegerExpr) {
                    sb.append(((SQLIntegerExpr) arg).getNumber());
                } else {
                    sb.append(arg.toString());
                }
                first = false;
            }
            sb.append(')');
        }
        return sb.toString();
    }

    /** Extracts column length. */
    private Integer extractLength(SQLColumnDefinition columnDef) {
        if (columnDef.getDataType() == null || columnDef.getDataType().getArguments() == null) {
            return null;
        }
        List<SQLExpr> args = columnDef.getDataType().getArguments();
        if (!args.isEmpty() && args.get(0) instanceof SQLIntegerExpr) {
            return ((SQLIntegerExpr) args.get(0)).getNumber().intValue();
        }
        return null;
    }

    /** Extracts precision. */
    private Integer extractPrecision(SQLColumnDefinition columnDef) {
        if (columnDef.getDataType() == null || columnDef.getDataType().getArguments() == null) {
            return null;
        }
        List<SQLExpr> args = columnDef.getDataType().getArguments();
        if (args.size() > 0 && args.get(0) instanceof SQLIntegerExpr) {
            return ((SQLIntegerExpr) args.get(0)).getNumber().intValue();
        }
        return null;
    }

    /** Extracts scale. */
    private Integer extractScale(SQLColumnDefinition columnDef) {
        if (columnDef.getDataType() == null || columnDef.getDataType().getArguments() == null) {
            return null;
        }
        List<SQLExpr> args = columnDef.getDataType().getArguments();
        if (args.size() > 1 && args.get(1) instanceof SQLIntegerExpr) {
            return ((SQLIntegerExpr) args.get(1)).getNumber().intValue();
        }
        return null;
    }

    /** Checks if column has NOT NULL constraint. */
    private boolean hasNotNullConstraint(SQLColumnDefinition columnDef) {
        List<SQLColumnConstraint> constraints = columnDef.getConstraints();
        if (constraints == null) {
            return false;
        }
        return constraints.stream()
                .anyMatch(
                        c ->
                                c instanceof SQLNotNullConstraint
                                        || c.toString().contains("NOT NULL"));
    }

    /** Extracts default value. */
    private String extractDefaultValue(SQLColumnDefinition columnDef) {
        SQLExpr defaultExpr = columnDef.getDefaultExpr();
        if (defaultExpr == null || defaultExpr instanceof SQLNullExpr) {
            return null;
        }
        return defaultExpr.toString();
    }

    /** Extracts column comment. */
    private String extractColumnComment(SQLColumnDefinition columnDef) {
        if (columnDef.getComment() instanceof SQLTextLiteralExpr) {
            return ((SQLTextLiteralExpr) columnDef.getComment()).getText();
        }
        return null;
    }

    /** Extracts table comment. */
    private String extractTableComment(SQLCreateTableStatement statement) {
        if (statement.getComment() instanceof SQLTextLiteralExpr) {
            return ((SQLTextLiteralExpr) statement.getComment()).getText();
        }
        return null;
    }

    /** Extracts character set. */
    private String extractCharacterSet(SQLColumnDefinition columnDef) {
        SQLExpr charsetExpr = columnDef.getCharsetExpr();
        if (charsetExpr instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) charsetExpr).getName();
        }
        return null;
    }

    /** Extracts collation. */
    private String extractCollation(SQLColumnDefinition columnDef) {
        SQLExpr collateExpr = columnDef.getCollateExpr();
        if (collateExpr instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) collateExpr).getName();
        }
        return null;
    }

    /** Gets constraint name. */
    private String getConstraintName(SQLConstraint constraint) {
        if (constraint.getName() != null) {
            return extractIdentifier(constraint.getName());
        }
        return constraint.toString().split(" ")[0];
    }

    /** Determines DatabaseDialect from dbType string. */
    private DatabaseDialect determineDialect(String dbType) {
        switch (dbType.toLowerCase(Locale.ROOT)) {
            case "mysql":
                return DatabaseDialect.MYSQL;
            case "postgresql":
                return DatabaseDialect.POSTGRESQL;
            case "oracle":
                return DatabaseDialect.ORACLE;
            default:
                return DatabaseDialect.MYSQL;
        }
    }
}
