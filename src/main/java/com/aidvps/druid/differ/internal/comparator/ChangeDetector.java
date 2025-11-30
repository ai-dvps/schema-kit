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

package com.aidvps.druid.differ.internal.comparator;

import com.aidvps.druid.differ.internal.model.Column;
import com.aidvps.druid.differ.internal.model.ColumnDiff;
import com.aidvps.druid.differ.internal.model.Index;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.SchemaDiff;
import com.aidvps.druid.differ.internal.model.Table;
import com.aidvps.druid.differ.internal.model.TableDiff;
import com.aidvps.druid.differ.internal.model.constraint.Constraint;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects differences between two database schemas.
 *
 * <p>This class compares source and target schemas and produces a detailed SchemaDiff object
 * capturing all changes needed to transform the source schema into the target schema.
 */
public class ChangeDetector {

    /**
     * Compares two schemas and returns the differences.
     *
     * @param sourceSchema the source schema
     * @param targetSchema the target schema
     * @return a SchemaDiff object containing all detected changes
     */
    public SchemaDiff compare(Schema sourceSchema, Schema targetSchema) {
        SchemaDiff.Builder diffBuilder = new SchemaDiff.Builder(sourceSchema, targetSchema);

        // Fast path: check if schemas have same hash and are likely identical
        if (sourceSchema.hashCode() == targetSchema.hashCode()
                && schemasAreDeeplyEqual(sourceSchema, targetSchema)) {
            return diffBuilder.build(); // Return empty diff - schemas are identical
        }

        Set<String> sourceTableNames = sourceSchema.getTableNames();
        Set<String> targetTableNames = targetSchema.getTableNames();

        Set<String> addedTables = new HashSet<>(targetTableNames);
        addedTables.removeAll(sourceTableNames);

        Set<String> removedTables = new HashSet<>(sourceTableNames);
        removedTables.removeAll(targetTableNames);

        Set<String> commonTables = new HashSet<>(sourceTableNames);
        commonTables.retainAll(targetTableNames);

        for (String tableName : addedTables) {
            diffBuilder.addAddedTable(tableName, targetSchema.getTable(tableName).get());
        }

        for (String tableName : removedTables) {
            diffBuilder.addRemovedTable(tableName, sourceSchema.getTable(tableName).get());
        }

        // Compare common tables
        for (String tableName : commonTables) {
            Table sourceTable = sourceSchema.getTable(tableName).get();
            Table targetTable = targetSchema.getTable(tableName).get();

            TableDiff tableDiff = compareTables(tableName, sourceTable, targetTable);
            if (!tableDiff.isEmpty()) {
                diffBuilder.addModifiedTable(tableName, tableDiff);
            }
        }

        return diffBuilder.build();
    }

    /**
     * Performs a deep equality check between two schemas. This is more thorough than
     * Schema.equals() which only compares names.
     *
     * @param schema1 the first schema
     * @param schema2 the second schema
     * @return true if schemas are deeply equal
     */
    private boolean schemasAreDeeplyEqual(Schema schema1, Schema schema2) {
        if (schema1 == schema2) return true;
        if (schema1 == null || schema2 == null) return false;
        if (schema1.getTables().size() != schema2.getTables().size()) return false;

        // Check each table deeply
        for (String tableName : schema1.getTableNames()) {
            if (!schema2.getTable(tableName).isPresent()) return false;

            Table table1 = schema1.getTable(tableName).get();
            Table table2 = schema2.getTable(tableName).get();

            if (!tablesAreDeeplyEqual(table1, table2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Performs a deep equality check between two tables. This is more thorough than Table.equals()
     * which only compares names.
     *
     * @param table1 the first table
     * @param table2 the second table
     * @return true if tables are deeply equal
     */
    private boolean tablesAreDeeplyEqual(Table table1, Table table2) {
        if (table1 == table2) return true;
        if (table1 == null || table2 == null) return false;

        // Check columns
        if (table1.getColumns().size() != table2.getColumns().size()) return false;

        for (Column column1 : table1.getColumns()) {
            boolean found = false;
            for (Column column2 : table2.getColumns()) {
                if (columnsAreEqual(column1, column2)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        // Check constraints
        if (table1.getConstraints().size() != table2.getConstraints().size()) return false;

        for (String constraintName : table1.getConstraints().keySet()) {
            if (!table2.getConstraints().containsKey(constraintName)) return false;
            Constraint c1 = table1.getConstraints().get(constraintName);
            Constraint c2 = table2.getConstraints().get(constraintName);
            if (!c1.equals(c2)) return false;
        }

        // Check table comment
        if (!equalsIgnoreCaseAndWhitespace(
                table1.getComment().orElse(null), table2.getComment().orElse(null))) {
            return false;
        }

        // Check table options
        if (table1.getOptions().size() != table2.getOptions().size()) return false;

        for (String key : table1.getOptions().keySet()) {
            if (!table2.getOptions().containsKey(key)) return false;
            if (!equalsIgnoreCaseAndWhitespace(
                    table1.getOptions().get(key), table2.getOptions().get(key))) {
                return false;
            }
        }

        // Check indexes
        if (table1.getIndexes().size() != table2.getIndexes().size()) return false;

        for (Index index1 : table1.getIndexes()) {
            boolean found = false;
            for (Index index2 : table2.getIndexes()) {
                if (indexesAreEqual(index1, index2)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        return true;
    }

    /** Checks if two columns are equal. */
    private boolean columnsAreEqual(Column c1, Column c2) {
        return c1.getName().equals(c2.getName())
                && c1.getDataType().equals(c2.getDataType())
                && c1.isNullable() == c2.isNullable()
                && Objects.equals(c1.getDefaultValue(), c2.getDefaultValue());
    }

    /** Checks if two indexes are equal. */
    private boolean indexesAreEqual(Index i1, Index i2) {
        if (!i1.getName().equals(i2.getName())) return false;
        if (i1.isUnique() != i2.isUnique()) return false;
        if (i1.getColumns().size() != i2.getColumns().size()) return false;
        for (int i = 0; i < i1.getColumns().size(); i++) {
            if (!i1.getColumns().get(i).equals(i2.getColumns().get(i))) return false;
        }
        return true;
    }

    /**
     * Compares two tables and returns the differences.
     *
     * @param tableName the table name
     * @param sourceTable the source table
     * @param targetTable the target table
     * @return a TableDiff object containing all detected changes
     */
    private TableDiff compareTables(String tableName, Table sourceTable, Table targetTable) {
        TableDiff.Builder diffBuilder = new TableDiff.Builder(tableName, sourceTable, targetTable);

        Set<String> sourceColumnNames = new HashSet<>(sourceTable.getColumnNames());
        Set<String> targetColumnNames = new HashSet<>(targetTable.getColumnNames());

        Set<String> addedColumns = new HashSet<>(targetColumnNames);
        addedColumns.removeAll(sourceColumnNames);

        Set<String> removedColumns = new HashSet<>(sourceColumnNames);
        removedColumns.removeAll(targetColumnNames);

        Set<String> commonColumns = new HashSet<>(sourceColumnNames);
        commonColumns.retainAll(targetColumnNames);

        for (String columnName : addedColumns) {
            diffBuilder.addAddedColumn(targetTable.getColumn(columnName).get());
        }

        for (String columnName : removedColumns) {
            diffBuilder.addRemovedColumn(columnName);
        }

        for (String columnName : commonColumns) {
            Column sourceColumn = sourceTable.getColumn(columnName).get();
            Column targetColumn = targetTable.getColumn(columnName).get();

            ColumnDiff columnDiff = compareColumns(sourceColumn, targetColumn);
            if (!columnDiff.isEmpty()) {
                diffBuilder.addModifiedColumn(columnName, columnDiff);
            }
        }

        compareConstraints(tableName, sourceTable, targetTable, diffBuilder);
        compareIndexes(tableName, sourceTable, targetTable, diffBuilder);
        compareTableProperties(tableName, sourceTable, targetTable, diffBuilder);

        return diffBuilder.build();
    }

    /**
     * Compares two columns and returns the differences.
     *
     * @param sourceColumn the source column
     * @param targetColumn the target column
     * @return a ColumnDiff object containing all detected changes
     */
    private ColumnDiff compareColumns(Column sourceColumn, Column targetColumn) {
        ColumnDiff.Builder diffBuilder = new ColumnDiff.Builder(sourceColumn, targetColumn);

        if (!equalsIgnoreCaseAndWhitespace(
                sourceColumn.getDataType(), targetColumn.getDataType())) {
            diffBuilder.addChange(ColumnDiff.ChangeType.DATA_TYPE_CHANGED);
        }

        if (!areDefaultValuesSemanticallyEquivalent(
                sourceColumn.getDefaultValue().orElse(null),
                targetColumn.getDefaultValue().orElse(null),
                sourceColumn.getDataType())) {
            diffBuilder.addChange(ColumnDiff.ChangeType.DEFAULT_VALUE_CHANGED);
        }

        if (sourceColumn.isNullable() != targetColumn.isNullable()) {
            diffBuilder.addChange(ColumnDiff.ChangeType.NULLABILITY_CHANGED);
        }

        if (sourceColumn.isAutoIncrement() != targetColumn.isAutoIncrement()) {
            diffBuilder.addChange(ColumnDiff.ChangeType.AUTO_INCREMENT_CHANGED);
        }

        if (!equalsIgnoreCaseAndWhitespace(
                sourceColumn.getLength().map(Object::toString).orElse(null),
                targetColumn.getLength().map(Object::toString).orElse(null))) {
            diffBuilder.addChange(ColumnDiff.ChangeType.LENGTH_CHANGED);
        }

        if (!equalsIgnoreCaseAndWhitespace(
                sourceColumn.getPrecision().map(Object::toString).orElse(null),
                targetColumn.getPrecision().map(Object::toString).orElse(null))) {
            diffBuilder.addChange(ColumnDiff.ChangeType.PRECISION_CHANGED);
        }

        if (!equalsIgnoreCaseAndWhitespace(
                sourceColumn.getScale().map(Object::toString).orElse(null),
                targetColumn.getScale().map(Object::toString).orElse(null))) {
            diffBuilder.addChange(ColumnDiff.ChangeType.SCALE_CHANGED);
        }

        if (!equalsIgnoreCaseAndWhitespace(
                sourceColumn.getComment().orElse(null), targetColumn.getComment().orElse(null))) {
            diffBuilder.addChange(ColumnDiff.ChangeType.COMMENT_CHANGED);
        }

        if (!equalsIgnoreCaseAndWhitespace(
                sourceColumn.getCharacterSet().orElse(null),
                targetColumn.getCharacterSet().orElse(null))) {
            diffBuilder.addChange(ColumnDiff.ChangeType.CHARACTER_SET_CHANGED);
        }

        if (!equalsIgnoreCaseAndWhitespace(
                sourceColumn.getCollation().orElse(null),
                targetColumn.getCollation().orElse(null))) {
            diffBuilder.addChange(ColumnDiff.ChangeType.COLLATION_CHANGED);
        }

        return diffBuilder.build();
    }

    /** Compares constraints between two tables. */
    private void compareConstraints(
            String tableName, Table sourceTable, Table targetTable, TableDiff.Builder diffBuilder) {
        Set<String> sourceConstraintNames = sourceTable.getConstraints().keySet();
        Set<String> targetConstraintNames = targetTable.getConstraints().keySet();

        Set<String> addedConstraints = new HashSet<>(targetConstraintNames);
        addedConstraints.removeAll(sourceConstraintNames);

        Set<String> removedConstraints = new HashSet<>(sourceConstraintNames);
        removedConstraints.removeAll(targetConstraintNames);

        Set<String> commonConstraints = new HashSet<>(sourceConstraintNames);
        commonConstraints.retainAll(targetConstraintNames);

        for (String constraintName : addedConstraints) {
            diffBuilder.addAddedConstraint(targetTable.getConstraints().get(constraintName));
        }

        for (String constraintName : removedConstraints) {
            diffBuilder.addRemovedConstraint(constraintName);
        }

        for (String constraintName : commonConstraints) {
            Constraint sourceConstraint = sourceTable.getConstraints().get(constraintName);
            Constraint targetConstraint = targetTable.getConstraints().get(constraintName);

            if (!sourceConstraint.equals(targetConstraint)) {
                diffBuilder.addRemovedConstraint(constraintName);
                diffBuilder.addAddedConstraint(targetConstraint);
            }
        }
    }

    /** Compares indexes between two tables. */
    private void compareIndexes(
            String tableName, Table sourceTable, Table targetTable, TableDiff.Builder diffBuilder) {
        Set<String> sourceIndexNames =
                sourceTable.getIndexes().stream()
                        .map(Index::getName)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());
        Set<String> targetIndexNames =
                targetTable.getIndexes().stream()
                        .map(Index::getName)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());

        Set<String> addedIndexes = new HashSet<>(targetIndexNames);
        addedIndexes.removeAll(sourceIndexNames);

        Set<String> removedIndexes = new HashSet<>(sourceIndexNames);
        removedIndexes.removeAll(targetIndexNames);

        for (String indexName : addedIndexes) {
            targetTable.getIndex(indexName).ifPresent(diffBuilder::addAddedIndex);
        }

        for (String indexName : removedIndexes) {
            diffBuilder.addRemovedIndex(indexName);
        }
    }

    /** Compares table properties (comment, options) between two tables. */
    private void compareTableProperties(
            String tableName, Table sourceTable, Table targetTable, TableDiff.Builder diffBuilder) {
        // Compare table comments
        String sourceComment = sourceTable.getComment().orElse(null);
        String targetComment = targetTable.getComment().orElse(null);

        if (!equalsIgnoreCaseAndWhitespace(sourceComment, targetComment)) {
            diffBuilder.commentChanged(sourceComment, targetComment);
        }

        // Compare table options (engine, tablespace, etc.)
        Map<String, String> sourceOptions = sourceTable.getOptions();
        Map<String, String> targetOptions = targetTable.getOptions();

        // Find added options
        for (Map.Entry<String, String> entry : targetOptions.entrySet()) {
            String key = entry.getKey();
            String targetValue = entry.getValue();

            if (!sourceOptions.containsKey(key)) {
                diffBuilder.addAddedOption(key, targetValue);
            } else {
                // Check if value changed
                String sourceValue = sourceOptions.get(key);
                if (!equalsIgnoreCaseAndWhitespace(sourceValue, targetValue)) {
                    diffBuilder.addModifiedOption(key, targetValue);
                }
            }
        }

        // Find removed options
        for (Map.Entry<String, String> entry : sourceOptions.entrySet()) {
            String key = entry.getKey();
            String sourceValue = entry.getValue();

            if (!targetOptions.containsKey(key)) {
                diffBuilder.addRemovedOption(key, sourceValue);
            }
        }
    }

    /**
     * Checks if two default values are semantically equivalent.
     *
     * <p>This method performs semantic equivalence checking rather than exact string matching. For
     * example: - NULL and null are equivalent - CURRENT_TIMESTAMP and CURRENT_TIMESTAMP() are
     * equivalent (for timestamp types) - 0 and 0.0 are equivalent (for numeric types)
     *
     * @param default1 the first default value
     * @param default2 the second default value
     * @param dataType the column data type
     * @return true if the defaults are semantically equivalent
     */
    private boolean areDefaultValuesSemanticallyEquivalent(
            String default1, String default2, String dataType) {
        // Both null
        if (default1 == null && default2 == null) {
            return true;
        }

        // One null, one not
        if (default1 == null || default2 == null) {
            return false;
        }

        // Normalize both values
        String norm1 = normalizeDefaultValue(default1, dataType);
        String norm2 = normalizeDefaultValue(default2, dataType);

        return norm1.equals(norm2);
    }

    /**
     * Normalizes a default value for semantic comparison.
     *
     * @param value the default value
     * @param dataType the column data type
     * @return the normalized value
     */
    private String normalizeDefaultValue(String value, String dataType) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();

        // Case-insensitive comparison for certain functions
        String upper = normalized.toUpperCase();

        // CURRENT_TIMESTAMP variations
        if (upper.startsWith("CURRENT_TIMESTAMP")) {
            return "CURRENT_TIMESTAMP";
        }

        // NULL variations
        if (upper.equals("NULL")) {
            return "NULL";
        }

        // Numeric values - normalize spacing
        if (isNumericType(dataType)) {
            return normalized.replaceAll("\\s+", "");
        }

        // String literals - remove extra quotes spacing
        if (normalized.startsWith("'") && normalized.endsWith("'")) {
            // Normalize escaping
            return normalized.replace("''", "'");
        }

        return normalized;
    }

    /**
     * Checks if a data type is numeric.
     *
     * @param dataType the data type
     * @return true if numeric
     */
    private boolean isNumericType(String dataType) {
        if (dataType == null) {
            return false;
        }
        String upper = dataType.toUpperCase();
        return upper.contains("INT")
                || upper.contains("DECIMAL")
                || upper.contains("NUMERIC")
                || upper.contains("FLOAT")
                || upper.contains("DOUBLE")
                || upper.contains("REAL")
                || upper.contains("BIGINT")
                || upper.contains("SMALLINT")
                || upper.contains("TINYINT")
                || upper.contains("MEDIUMINT")
                || upper.contains("NUMERIC");
    }

    /** Compares two strings for equality, ignoring case and whitespace. */
    private boolean equalsIgnoreCaseAndWhitespace(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return normalize(s1).equals(normalize(s2));
    }

    /** Normalizes a string by trimming and removing extra whitespace. */
    private String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }
}
