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
        if (sourceSchema.equals(targetSchema)) {
            return SchemaDiff.empty(sourceSchema);
        }

        SchemaDiff.Builder diffBuilder = new SchemaDiff.Builder(sourceSchema, targetSchema);

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

        if (!equalsIgnoreCaseAndWhitespace(
                sourceColumn.getDefaultValue().orElse(null),
                targetColumn.getDefaultValue().orElse(null))) {
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
