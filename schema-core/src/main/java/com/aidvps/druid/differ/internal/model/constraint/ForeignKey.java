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

package com.aidvps.druid.differ.internal.model.constraint;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a foreign key constraint.
 *
 * <p>A foreign key establishes a link between data in two tables, enforcing referential integrity.
 */
public final class ForeignKey extends Constraint {
    private final List<String> columns;
    private final String referencedTable;
    private final List<String> referencedColumns;
    private final OnAction onDelete;
    private final OnAction onUpdate;

    /** Actions that can be taken when a referenced row is deleted or updated. */
    public enum OnAction {
        CASCADE,
        SET_NULL,
        RESTRICT,
        NO_ACTION
    }

    /**
     * Creates a new ForeignKey constraint.
     *
     * @param name the constraint name (may be null)
     * @param columns the columns in the foreign key
     * @param referencedTable the name of the referenced table
     * @param referencedColumns the columns in the referenced table
     * @param onDelete the action to take on delete (may be null)
     * @param onUpdate the action to take on update (may be null)
     * @param enabled whether the constraint is enabled
     * @param deferred whether the constraint is deferred
     */
    public ForeignKey(
            String name,
            List<String> columns,
            String referencedTable,
            List<String> referencedColumns,
            OnAction onDelete,
            OnAction onUpdate,
            boolean enabled,
            boolean deferred) {
        super(name, enabled, deferred);
        Objects.requireNonNull(columns, "Foreign key columns cannot be null");
        Objects.requireNonNull(referencedTable, "Referenced table cannot be null");
        Objects.requireNonNull(referencedColumns, "Referenced columns cannot be null");
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Foreign key must have at least one column");
        }
        if (referencedColumns.isEmpty()) {
            throw new IllegalArgumentException("Referenced columns cannot be empty");
        }
        this.columns = Collections.unmodifiableList(columns);
        this.referencedTable = referencedTable;
        this.referencedColumns = Collections.unmodifiableList(referencedColumns);
        this.onDelete = onDelete;
        this.onUpdate = onUpdate;
    }

    /**
     * Returns the columns in the foreign key.
     *
     * @return an unmodifiable list of column names
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Returns the name of the referenced table.
     *
     * @return the referenced table name (never null)
     */
    public String getReferencedTable() {
        return referencedTable;
    }

    /**
     * Returns the columns in the referenced table.
     *
     * @return an unmodifiable list of column names
     */
    public List<String> getReferencedColumns() {
        return referencedColumns;
    }

    /**
     * Returns the action to take on delete.
     *
     * @return an Optional containing the action, or empty if not specified
     */
    public Optional<OnAction> getOnDelete() {
        return Optional.ofNullable(onDelete);
    }

    /**
     * Returns the action to take on update.
     *
     * @return an Optional containing the action, or empty if not specified
     */
    public Optional<OnAction> getOnUpdate() {
        return Optional.ofNullable(onUpdate);
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.FOREIGN_KEY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForeignKey that = (ForeignKey) o;
        return columns.equals(that.columns)
                && referencedTable.equals(that.referencedTable)
                && referencedColumns.equals(that.referencedColumns)
                && onDelete == that.onDelete
                && onUpdate == that.onUpdate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns, referencedTable, referencedColumns, onDelete, onUpdate);
    }

    @Override
    public String toString() {
        StringBuilder sb =
                new StringBuilder("FOREIGN KEY (")
                        .append(String.join(", ", columns))
                        .append(") REFERENCES ")
                        .append(referencedTable)
                        .append(" (")
                        .append(String.join(", ", referencedColumns))
                        .append(')');
        if (onDelete != null) {
            sb.append(" ON DELETE ").append(onDelete);
        }
        if (onUpdate != null) {
            sb.append(" ON UPDATE ").append(onUpdate);
        }
        return sb.toString();
    }

    /** Builder for creating ForeignKey constraints. */
    public static class Builder {
        private final java.util.List<String> columns = new java.util.ArrayList<>();
        private final java.util.List<String> referencedColumns = new java.util.ArrayList<>();
        private String name;
        private String referencedTable;
        private OnAction onDelete;
        private OnAction onUpdate;
        private boolean enabled = true;
        private boolean deferred = false;

        /**
         * Adds a column to the foreign key.
         *
         * @param column the column name
         * @return this Builder for chaining
         */
        public Builder addColumn(String column) {
            columns.add(column);
            return this;
        }

        /**
         * Sets the referenced table name.
         *
         * @param referencedTable the referenced table name
         * @return this Builder for chaining
         */
        public Builder referencedTable(String referencedTable) {
            this.referencedTable = referencedTable;
            return this;
        }

        /**
         * Adds a referenced column.
         *
         * @param column the referenced column name
         * @return this Builder for chaining
         */
        public Builder addReferencedColumn(String column) {
            referencedColumns.add(column);
            return this;
        }

        /**
         * Sets the ON DELETE action.
         *
         * @param onDelete the action
         * @return this Builder for chaining
         */
        public Builder onDelete(OnAction onDelete) {
            this.onDelete = onDelete;
            return this;
        }

        /**
         * Sets the ON UPDATE action.
         *
         * @param onUpdate the action
         * @return this Builder for chaining
         */
        public Builder onUpdate(OnAction onUpdate) {
            this.onUpdate = onUpdate;
            return this;
        }

        /**
         * Sets the constraint name.
         *
         * @param name the constraint name
         * @return this Builder for chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets whether the constraint is enabled.
         *
         * @param enabled true if enabled
         * @return this Builder for chaining
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets whether the constraint is deferred.
         *
         * @param deferred true if deferred
         * @return this Builder for chaining
         */
        public Builder deferred(boolean deferred) {
            this.deferred = deferred;
            return this;
        }

        /**
         * Builds the ForeignKey constraint.
         *
         * @return a new ForeignKey
         */
        public ForeignKey build() {
            return new ForeignKey(
                    name,
                    columns,
                    referencedTable,
                    referencedColumns,
                    onDelete,
                    onUpdate,
                    enabled,
                    deferred);
        }
    }
}
