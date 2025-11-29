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

package com.aidvps.druid.differ.internal.model;

import com.aidvps.druid.differ.internal.model.constraint.Constraint;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents differences between two table structures.
 *
 * <p>A TableDiff captures all changes needed to transform a source table into a target table,
 * including added columns, removed columns, modified columns, and changes to constraints and
 * indexes.
 */
public final class TableDiff {

    private final String tableName;
    private final Table sourceTable;
    private final Table targetTable;
    private final List<Column> addedColumns;
    private final List<String> removedColumns;
    private final Map<String, ColumnDiff> modifiedColumns;
    private final List<Constraint> addedConstraints;
    private final List<String> removedConstraints;
    private final List<Index> addedIndexes;
    private final List<String> removedIndexes;

    /**
     * Creates a new TableDiff.
     *
     * @param tableName the table name (never null)
     * @param sourceTable the source table (never null)
     * @param targetTable the target table (never null)
     * @param addedColumns columns that exist only in target
     * @param removedColumns column names that exist only in source
     * @param modifiedColumns columns that exist in both but have different definitions
     * @param addedConstraints constraints that exist only in target
     * @param removedConstraints constraint names that exist only in source
     * @param addedIndexes indexes that exist only in target
     * @param removedIndexes index names that exist only in source
     */
    public TableDiff(
            String tableName,
            Table sourceTable,
            Table targetTable,
            List<Column> addedColumns,
            List<String> removedColumns,
            Map<String, ColumnDiff> modifiedColumns,
            List<Constraint> addedConstraints,
            List<String> removedConstraints,
            List<Index> addedIndexes,
            List<String> removedIndexes) {
        this.tableName = Objects.requireNonNull(tableName, "Table name cannot be null");
        this.sourceTable = Objects.requireNonNull(sourceTable, "Source table cannot be null");
        this.targetTable = Objects.requireNonNull(targetTable, "Target table cannot be null");
        this.addedColumns = Collections.unmodifiableList(new java.util.ArrayList<>(addedColumns));
        this.removedColumns =
                Collections.unmodifiableList(new java.util.ArrayList<>(removedColumns));
        this.modifiedColumns =
                Collections.unmodifiableMap(new java.util.HashMap<>(modifiedColumns));
        this.addedConstraints =
                Collections.unmodifiableList(new java.util.ArrayList<>(addedConstraints));
        this.removedConstraints =
                Collections.unmodifiableList(new java.util.ArrayList<>(removedConstraints));
        this.addedIndexes = Collections.unmodifiableList(new java.util.ArrayList<>(addedIndexes));
        this.removedIndexes =
                Collections.unmodifiableList(new java.util.ArrayList<>(removedIndexes));
    }

    /**
     * Returns the table name.
     *
     * @return the table name (never null)
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Returns the source table.
     *
     * @return the source table (never null)
     */
    public Table getSourceTable() {
        return sourceTable;
    }

    /**
     * Returns the target table.
     *
     * @return the target table (never null)
     */
    public Table getTargetTable() {
        return targetTable;
    }

    /**
     * Returns columns that exist only in the target table.
     *
     * @return an unmodifiable list of columns
     */
    public List<Column> getAddedColumns() {
        return addedColumns;
    }

    /**
     * Returns column names that exist only in the source table.
     *
     * @return an unmodifiable list of column names
     */
    public List<String> getRemovedColumns() {
        return removedColumns;
    }

    /**
     * Returns columns that exist in both tables but have different definitions.
     *
     * @return an unmodifiable map of column name to ColumnDiff
     */
    public Map<String, ColumnDiff> getModifiedColumns() {
        return modifiedColumns;
    }

    /**
     * Returns a specific column difference.
     *
     * @param columnName the column name
     * @return an Optional containing the ColumnDiff if the column is modified, or empty
     */
    public Optional<ColumnDiff> getModifiedColumn(String columnName) {
        return Optional.ofNullable(modifiedColumns.get(columnName));
    }

    /**
     * Returns constraints that exist only in the target table.
     *
     * @return an unmodifiable list of constraints
     */
    public List<Constraint> getAddedConstraints() {
        return addedConstraints;
    }

    /**
     * Returns constraint names that exist only in the source table.
     *
     * @return an unmodifiable list of constraint names
     */
    public List<String> getRemovedConstraints() {
        return removedConstraints;
    }

    /**
     * Returns indexes that exist only in the target table.
     *
     * @return an unmodifiable list of indexes
     */
    public List<Index> getAddedIndexes() {
        return addedIndexes;
    }

    /**
     * Returns index names that exist only in the source table.
     *
     * @return an unmodifiable list of index names
     */
    public List<String> getRemovedIndexes() {
        return removedIndexes;
    }

    /**
     * Returns whether the tables are identical.
     *
     * @return true if there are no differences, false otherwise
     */
    public boolean isEmpty() {
        return addedColumns.isEmpty()
                && removedColumns.isEmpty()
                && modifiedColumns.isEmpty()
                && addedConstraints.isEmpty()
                && removedConstraints.isEmpty()
                && addedIndexes.isEmpty()
                && removedIndexes.isEmpty();
    }

    /**
     * Returns the total number of changes.
     *
     * @return the count of all changes
     */
    public int getChangeCount() {
        return addedColumns.size()
                + removedColumns.size()
                + modifiedColumns.size()
                + addedConstraints.size()
                + removedConstraints.size()
                + addedIndexes.size()
                + removedIndexes.size();
    }

    /**
     * Returns a summary of all changes.
     *
     * @return a human-readable summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Table Diff for: ").append(tableName).append("\n");

        if (!addedColumns.isEmpty()) {
            sb.append("  Added Columns (").append(addedColumns.size()).append("):\n");
            addedColumns.stream()
                    .map(Column::getName)
                    .forEach(name -> sb.append("    + ").append(name).append("\n"));
        }

        if (!removedColumns.isEmpty()) {
            sb.append("  Removed Columns (").append(removedColumns.size()).append("):\n");
            removedColumns.forEach(name -> sb.append("    - ").append(name).append("\n"));
        }

        if (!modifiedColumns.isEmpty()) {
            sb.append("  Modified Columns (").append(modifiedColumns.size()).append("):\n");
            modifiedColumns.keySet().stream()
                    .sorted()
                    .forEach(name -> sb.append("    ~ ").append(name).append("\n"));
        }

        if (!addedConstraints.isEmpty()) {
            sb.append("  Added Constraints (").append(addedConstraints.size()).append("):\n");
            addedConstraints.forEach(c -> sb.append("    + ").append(c.getType()).append("\n"));
        }

        if (!removedConstraints.isEmpty()) {
            sb.append("  Removed Constraints (").append(removedConstraints.size()).append("):\n");
            removedConstraints.forEach(name -> sb.append("    - ").append(name).append("\n"));
        }

        if (isEmpty()) {
            sb.append("  No differences found");
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableDiff that = (TableDiff) o;
        return tableName.equals(that.tableName)
                && sourceTable.equals(that.sourceTable)
                && targetTable.equals(that.targetTable)
                && addedColumns.equals(that.addedColumns)
                && removedColumns.equals(that.removedColumns)
                && modifiedColumns.equals(that.modifiedColumns)
                && addedConstraints.equals(that.addedConstraints)
                && removedConstraints.equals(that.removedConstraints)
                && addedIndexes.equals(that.addedIndexes)
                && removedIndexes.equals(that.removedIndexes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tableName,
                sourceTable,
                targetTable,
                addedColumns,
                removedColumns,
                modifiedColumns,
                addedConstraints,
                removedConstraints,
                addedIndexes,
                removedIndexes);
    }

    @Override
    public String toString() {
        return getSummary();
    }

    /** Builder for creating TableDiff instances. */
    public static class Builder {
        private final String tableName;
        private final Table sourceTable;
        private final Table targetTable;
        private final java.util.List<Column> addedColumns = new java.util.ArrayList<>();
        private final java.util.List<String> removedColumns = new java.util.ArrayList<>();
        private final java.util.Map<String, ColumnDiff> modifiedColumns = new java.util.HashMap<>();
        private final java.util.List<Constraint> addedConstraints = new java.util.ArrayList<>();
        private final java.util.List<String> removedConstraints = new java.util.ArrayList<>();
        private final java.util.List<Index> addedIndexes = new java.util.ArrayList<>();
        private final java.util.List<String> removedIndexes = new java.util.ArrayList<>();

        /**
         * Creates a new Builder.
         *
         * @param tableName the table name
         * @param sourceTable the source table
         * @param targetTable the target table
         */
        public Builder(String tableName, Table sourceTable, Table targetTable) {
            this.tableName = tableName;
            this.sourceTable = sourceTable;
            this.targetTable = targetTable;
        }

        /**
         * Adds a column that exists only in the target table.
         *
         * @param column the column
         * @return this Builder for chaining
         */
        public Builder addAddedColumn(Column column) {
            addedColumns.add(column);
            return this;
        }

        /**
         * Adds a column name that exists only in the source table.
         *
         * @param columnName the column name
         * @return this Builder for chaining
         */
        public Builder addRemovedColumn(String columnName) {
            removedColumns.add(columnName);
            return this;
        }

        /**
         * Adds a column that exists in both tables but has different definitions.
         *
         * @param columnName the column name
         * @param columnDiff the column differences
         * @return this Builder for chaining
         */
        public Builder addModifiedColumn(String columnName, ColumnDiff columnDiff) {
            modifiedColumns.put(columnName, columnDiff);
            return this;
        }

        /**
         * Adds a constraint that exists only in the target table.
         *
         * @param constraint the constraint
         * @return this Builder for chaining
         */
        public Builder addAddedConstraint(Constraint constraint) {
            addedConstraints.add(constraint);
            return this;
        }

        /**
         * Adds a constraint name that exists only in the source table.
         *
         * @param constraintName the constraint name
         * @return this Builder for chaining
         */
        public Builder addRemovedConstraint(String constraintName) {
            removedConstraints.add(constraintName);
            return this;
        }

        /**
         * Adds an index that exists only in the target table.
         *
         * @param index the index
         * @return this Builder for chaining
         */
        public Builder addAddedIndex(Index index) {
            addedIndexes.add(index);
            return this;
        }

        /**
         * Adds an index name that exists only in the source table.
         *
         * @param indexName the index name
         * @return this Builder for chaining
         */
        public Builder addRemovedIndex(String indexName) {
            removedIndexes.add(indexName);
            return this;
        }

        /**
         * Builds the TableDiff.
         *
         * @return a new TableDiff
         */
        public TableDiff build() {
            return new TableDiff(
                    tableName,
                    sourceTable,
                    targetTable,
                    addedColumns,
                    removedColumns,
                    modifiedColumns,
                    addedConstraints,
                    removedConstraints,
                    addedIndexes,
                    removedIndexes);
        }
    }
}
