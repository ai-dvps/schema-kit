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

package com.aidvps.druid.differ.internal.validator;

import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.Table;
import com.aidvps.druid.differ.internal.model.Warning;
import com.aidvps.druid.differ.internal.model.constraint.Constraint;
import com.aidvps.druid.differ.internal.model.constraint.ForeignKey;
import java.util.*;

/**
 * Validates schema and constraint definitions.
 *
 * <p>This class checks schemas for validity issues including:
 *
 * <ul>
 *   <li>Foreign key references to non-existent tables
 *   <li>Constraint columns that don't exist
 *   <li>Circular foreign key dependencies
 *   <li>Potentially dangerous operations
 * </ul>
 */
public class SchemaValidator {

    /** Validates a schema and returns any warnings. */
    public List<Warning> validate(Schema schema) {
        List<Warning> warnings = new ArrayList<>();

        // Validate foreign key references
        warnings.addAll(validateForeignKeyReferences(schema));

        // Validate constraint columns
        warnings.addAll(validateConstraintColumns(schema));

        // Validate data type compatibility
        warnings.addAll(validateDataTypeCompatibility(schema));

        // Detect circular dependencies
        warnings.addAll(validateCircularDependencies(schema));

        // Check for dangerous operations
        warnings.addAll(validateDangerousOperations(schema));

        return warnings;
    }

    /** Validates that all foreign keys reference existing tables. */
    private List<Warning> validateForeignKeyReferences(Schema schema) {
        List<Warning> warnings = new ArrayList<>();
        Set<String> tableNames = schema.getTableNames();

        for (Table table : schema.getTables().values()) {
            for (Constraint constraint : table.getConstraints().values()) {
                if (constraint instanceof ForeignKey) {
                    ForeignKey fk = (ForeignKey) constraint;
                    String referencedTable = fk.getReferencedTable();

                    if (!tableNames.contains(referencedTable)) {
                        warnings.add(
                                new Warning(
                                        Warning.Type.DEPENDENCY_NOTE,
                                        String.format(
                                                "Foreign key '%s' on table '%s' references non-existent table '%s'",
                                                constraint.getName().orElse("unnamed"),
                                                table.getName(),
                                                referencedTable),
                                        Warning.Severity.ERROR));
                    }
                }
            }
        }

        return warnings;
    }

    /** Validates that constraint columns exist in the table. */
    private List<Warning> validateConstraintColumns(Schema schema) {
        List<Warning> warnings = new ArrayList<>();

        for (Table table : schema.getTables().values()) {
            Set<String> columnNames = new HashSet<>(table.getColumnNames());
            String tableName = table.getName();

            for (Constraint constraint : table.getConstraints().values()) {
                if (constraint instanceof ForeignKey) {
                    ForeignKey fk = (ForeignKey) constraint;

                    // Check foreign key columns exist
                    for (String column : fk.getColumns()) {
                        if (!columnNames.contains(column)) {
                            warnings.add(
                                    new Warning(
                                            Warning.Type.COMPATIBILITY_WARNING,
                                            String.format(
                                                    "Foreign key '%s' on table '%s' references non-existent column '%s'",
                                                    constraint.getName().orElse("unnamed"),
                                                    tableName,
                                                    column),
                                            Warning.Severity.ERROR));
                        }
                    }

                    // Check referenced columns exist in referenced table
                    Table referencedTable = schema.getTable(fk.getReferencedTable()).orElse(null);
                    if (referencedTable != null) {
                        Set<String> referencedColumnNames =
                                new HashSet<>(referencedTable.getColumnNames());
                        for (String column : fk.getReferencedColumns()) {
                            if (!referencedColumnNames.contains(column)) {
                                warnings.add(
                                        new Warning(
                                                Warning.Type.COMPATIBILITY_WARNING,
                                                String.format(
                                                        "Foreign key '%s' references non-existent column '%s.%s'",
                                                        constraint.getName().orElse("unnamed"),
                                                        fk.getReferencedTable(),
                                                        column),
                                                Warning.Severity.ERROR));
                            }
                        }
                    }
                }
            }
        }

        return warnings;
    }

    /** Validates data type compatibility for foreign keys. */
    private List<Warning> validateDataTypeCompatibility(Schema schema) {
        List<Warning> warnings = new ArrayList<>();

        for (Table table : schema.getTables().values()) {
            String tableName = table.getName();

            for (Constraint constraint : table.getConstraints().values()) {
                if (constraint instanceof ForeignKey) {
                    ForeignKey fk = (ForeignKey) constraint;
                    Table referencedTable = schema.getTable(fk.getReferencedTable()).orElse(null);

                    if (referencedTable != null) {
                        // Check that foreign key and referenced columns have compatible types
                        for (int i = 0;
                                i < fk.getColumns().size() && i < fk.getReferencedColumns().size();
                                i++) {
                            String fkColumnName = fk.getColumns().get(i);
                            String referencedColumnName = fk.getReferencedColumns().get(i);

                            Optional<com.aidvps.druid.differ.internal.model.Column> fkColumn =
                                    table.getColumn(fkColumnName);
                            Optional<com.aidvps.druid.differ.internal.model.Column>
                                    referencedColumn =
                                            referencedTable.getColumn(referencedColumnName);

                            if (fkColumn.isPresent() && referencedColumn.isPresent()) {
                                String fkType = fkColumn.get().getDataType();
                                String referencedType = referencedColumn.get().getDataType();

                                // Check type compatibility (simplified)
                                if (!areTypesCompatible(fkType, referencedType)) {
                                    warnings.add(
                                            new Warning(
                                                    Warning.Type.COMPATIBILITY_WARNING,
                                                    String.format(
                                                            "Column '%s.%s' (type: %s) references '%s.%s' (type: %s) - incompatible types",
                                                            tableName,
                                                            fkColumnName,
                                                            fkType,
                                                            fk.getReferencedTable(),
                                                            referencedColumnName,
                                                            referencedType),
                                                    Warning.Severity.WARN));
                                }

                                // Check precision/scale for DECIMAL types
                                if (fkType.equalsIgnoreCase("DECIMAL")
                                        && referencedType.equalsIgnoreCase("DECIMAL")) {
                                    Integer fkPrecision =
                                            fkColumn.get().getPrecision().orElse(null);
                                    Integer fkScale = fkColumn.get().getScale().orElse(null);
                                    Integer refPrecision =
                                            referencedColumn.get().getPrecision().orElse(null);
                                    Integer refScale =
                                            referencedColumn.get().getScale().orElse(null);

                                    if (fkPrecision != null
                                            && refPrecision != null
                                            && !fkPrecision.equals(refPrecision)) {
                                        warnings.add(
                                                new Warning(
                                                        Warning.Type.COMPATIBILITY_WARNING,
                                                        String.format(
                                                                "Column '%s.%s' has different precision than referenced column '%s.%s'",
                                                                tableName,
                                                                fkColumnName,
                                                                fk.getReferencedTable(),
                                                                referencedColumnName),
                                                        Warning.Severity.INFO));
                                    }

                                    if (fkScale != null
                                            && refScale != null
                                            && !fkScale.equals(refScale)) {
                                        warnings.add(
                                                new Warning(
                                                        Warning.Type.COMPATIBILITY_WARNING,
                                                        String.format(
                                                                "Column '%s.%s' has different scale than referenced column '%s.%s'",
                                                                tableName,
                                                                fkColumnName,
                                                                fk.getReferencedTable(),
                                                                referencedColumnName),
                                                        Warning.Severity.INFO));
                                    }
                                }

                                // Check length for VARCHAR types
                                if (fkType.equalsIgnoreCase("VARCHAR")
                                        && referencedType.equalsIgnoreCase("VARCHAR")) {
                                    Integer fkLength = fkColumn.get().getLength().orElse(null);
                                    Integer refLength =
                                            referencedColumn.get().getLength().orElse(null);

                                    if (fkLength != null
                                            && refLength != null
                                            && fkLength > refLength) {
                                        warnings.add(
                                                new Warning(
                                                        Warning.Type.COMPATIBILITY_WARNING,
                                                        String.format(
                                                                "Column '%s.%s' has greater length than referenced column '%s.%s'",
                                                                tableName,
                                                                fkColumnName,
                                                                fk.getReferencedTable(),
                                                                referencedColumnName),
                                                        Warning.Severity.INFO));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return warnings;
    }

    /** Checks if two data types are compatible for foreign key relationships. */
    private boolean areTypesCompatible(String type1, String type2) {
        if (type1 == null || type2 == null) {
            return false;
        }

        String t1 = type1.toUpperCase();
        String t2 = type2.toUpperCase();

        // Exact match
        if (t1.equals(t2)) {
            return true;
        }

        // Compatible integer types
        if (isIntegerType(t1) && isIntegerType(t2)) {
            return true;
        }

        // Compatible floating point types
        if (isFloatingPointType(t1) && isFloatingPointType(t2)) {
            return true;
        }

        // VARCHAR and CHAR are generally compatible
        if ((t1.startsWith("VARCHAR") || t1.startsWith("CHAR"))
                && (t2.startsWith("VARCHAR") || t2.startsWith("CHAR"))) {
            return true;
        }

        // TEXT types are compatible with each other
        if ((t1.equals("TEXT") || t1.equals("CLOB")) && (t2.equals("TEXT") || t2.equals("CLOB"))) {
            return true;
        }

        // BLOB types are compatible with each other
        if ((t1.equals("BLOB")
                        || t1.equals("TINYBLOB")
                        || t1.equals("MEDIUMBLOB")
                        || t1.equals("LONGBLOB"))
                && (t2.equals("BLOB")
                        || t2.equals("TINYBLOB")
                        || t2.equals("MEDIUMBLOB")
                        || t2.equals("LONGBLOB"))) {
            return true;
        }

        return false;
    }

    /** Checks if a type is an integer type. */
    private boolean isIntegerType(String type) {
        String t = type.toUpperCase();
        return t.contains("INT")
                || t.equals("BIGINT")
                || t.equals("SMALLINT")
                || t.equals("TINYINT")
                || t.equals("MEDIUMINT");
    }

    /** Checks if a type is a floating point type. */
    private boolean isFloatingPointType(String type) {
        String t = type.toUpperCase();
        return t.equals("DECIMAL")
                || t.equals("NUMERIC")
                || t.equals("FLOAT")
                || t.equals("DOUBLE")
                || t.equals("REAL");
    }

    /** Detects circular foreign key dependencies. */
    private List<Warning> validateCircularDependencies(Schema schema) {
        List<Warning> warnings = new ArrayList<>();

        // Build dependency graph
        Map<String, Set<String>> dependencies = new HashMap<>();
        Map<String, Map<String, ForeignKey>> foreignKeys = new HashMap<>();

        for (Table table : schema.getTables().values()) {
            String tableName = table.getName();
            dependencies.put(tableName, new HashSet<>());
            foreignKeys.put(tableName, new HashMap<>());

            for (Constraint constraint : table.getConstraints().values()) {
                if (constraint instanceof ForeignKey) {
                    ForeignKey fk = (ForeignKey) constraint;
                    String referencedTable = fk.getReferencedTable();

                    if (dependencies.containsKey(referencedTable)) {
                        dependencies.get(tableName).add(referencedTable);
                        foreignKeys.get(tableName).put(fk.getName().orElse("unnamed"), fk);
                    }
                }
            }
        }

        // Detect cycles using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (String tableName : dependencies.keySet()) {
            if (!visited.contains(tableName)) {
                if (detectCycle(tableName, dependencies, visited, recStack, new Stack<>())) {
                    warnings.add(
                            new Warning(
                                    Warning.Type.DEPENDENCY_NOTE,
                                    "Circular foreign key dependency detected in tables: "
                                            + String.join(", ", recStack),
                                    Warning.Severity.ERROR));
                }
            }
        }

        return warnings;
    }

    /**
     * Detects cycle using DFS.
     *
     * @param node the current node
     * @param dependencies the dependency graph
     * @param visited visited nodes
     * @param recStack recursion stack
     * @param path current path
     * @return true if cycle detected
     */
    private boolean detectCycle(
            String node,
            Map<String, Set<String>> dependencies,
            Set<String> visited,
            Set<String> recStack,
            Stack<String> path) {
        visited.add(node);
        recStack.add(node);
        path.push(node);

        for (String neighbor : dependencies.getOrDefault(node, Collections.emptySet())) {
            if (!visited.contains(neighbor)) {
                if (detectCycle(neighbor, dependencies, visited, recStack, path)) {
                    return true;
                }
            } else if (recStack.contains(neighbor)) {
                return true;
            }
        }

        recStack.remove(node);
        path.pop();
        return false;
    }

    /** Validates for potentially dangerous operations. */
    private List<Warning> validateDangerousOperations(Schema schema) {
        List<Warning> warnings = new ArrayList<>();

        for (Table table : schema.getTables().values()) {
            String tableName = table.getName();

            // Check for large columns being dropped
            // This is a simplified check - in practice you'd track deleted columns
            if (hasLargeColumns(table)) {
                warnings.add(
                        new Warning(
                                Warning.Type.DATA_LOSS_RISK,
                                String.format(
                                        "Table '%s' contains large columns. Dropping columns may cause data loss.",
                                        tableName),
                                Warning.Severity.WARN));
            }

            // Check for potentially slow operations
            if (hasManyIndexes(table)) {
                warnings.add(
                        new Warning(
                                Warning.Type.PERFORMANCE_NOTE,
                                String.format(
                                        "Table '%s' has many indexes. This may impact write performance.",
                                        tableName),
                                Warning.Severity.INFO));
            }

            // Check for unsupported features
            for (Constraint constraint : table.getConstraints().values()) {
                if (constraint.getName().isPresent()
                        && constraint.getName().get().equals("CHECK")) {
                    warnings.add(
                            new Warning(
                                    Warning.Type.COMPATIBILITY_WARNING,
                                    String.format(
                                            "CHECK constraints on table '%s' may not be enforced in all MySQL versions.",
                                            tableName),
                                    Warning.Severity.INFO));
                }
            }
        }

        return warnings;
    }

    /** Checks if a table has large columns. */
    private boolean hasLargeColumns(Table table) {
        return table.getColumns().stream()
                .anyMatch(
                        column ->
                                column.getDataType().equalsIgnoreCase("BLOB")
                                        || column.getDataType().equalsIgnoreCase("TEXT")
                                        || column.getLength().orElse(0) > 1000);
    }

    /** Checks if a table has many indexes. */
    private boolean hasManyIndexes(Table table) {
        int indexCount = table.getIndexes().size();
        return indexCount > 10;
    }
}
