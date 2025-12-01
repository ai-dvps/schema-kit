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

/**
 * Represents a primary key constraint.
 *
 * <p>A primary key uniquely identifies each row in a table and cannot contain NULL values.
 */
public final class PrimaryKey extends Constraint {
    private final List<String> columns;

    /**
     * Creates a new PrimaryKey constraint.
     *
     * @param name the constraint name (may be null)
     * @param columns the list of columns in the primary key (must not be empty)
     * @param enabled whether the constraint is enabled
     * @param deferred whether the constraint is deferred
     */
    public PrimaryKey(String name, List<String> columns, boolean enabled, boolean deferred) {
        super(name, enabled, deferred);
        Objects.requireNonNull(columns, "Primary key columns cannot be null");
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Primary key must have at least one column");
        }
        this.columns = Collections.unmodifiableList(columns);
    }

    /**
     * Creates a new PrimaryKey constraint with default settings (enabled, not deferred).
     *
     * @param name the constraint name (may be null)
     * @param columns the list of columns in the primary key (must not be empty)
     */
    public PrimaryKey(String name, List<String> columns) {
        this(name, columns, true, false);
    }

    /**
     * Returns the columns in the primary key.
     *
     * @return an unmodifiable list of column names
     */
    public List<String> getColumns() {
        return columns;
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.PRIMARY_KEY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimaryKey that = (PrimaryKey) o;
        return columns.equals(that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns);
    }

    @Override
    public String toString() {
        return "PRIMARY KEY (" + String.join(", ", columns) + ")";
    }

    /** Builder for creating PrimaryKey constraints. */
    public static class Builder {
        private final java.util.List<String> columns = new java.util.ArrayList<>();
        private String name;
        private boolean enabled = true;
        private boolean deferred = false;

        /**
         * Adds a column to the primary key.
         *
         * @param column the column name
         * @return this Builder for chaining
         */
        public Builder addColumn(String column) {
            columns.add(column);
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
         * Builds the PrimaryKey constraint.
         *
         * @return a new PrimaryKey
         */
        public PrimaryKey build() {
            return new PrimaryKey(name, columns, enabled, deferred);
        }
    }
}
