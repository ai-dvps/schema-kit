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
 * Represents a unique constraint.
 *
 * <p>A unique constraint ensures that all non-NULL values in a column or set of columns are unique.
 */
public final class UniqueConstraint extends Constraint {
    private final List<String> columns;

    /**
     * Creates a new UniqueConstraint.
     *
     * @param name the constraint name (may be null)
     * @param columns the columns in the unique constraint (must not be empty)
     * @param enabled whether the constraint is enabled
     * @param deferred whether the constraint is deferred
     */
    public UniqueConstraint(String name, List<String> columns, boolean enabled, boolean deferred) {
        super(name, enabled, deferred);
        Objects.requireNonNull(columns, "Unique constraint columns cannot be null");
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Unique constraint must have at least one column");
        }
        this.columns = Collections.unmodifiableList(columns);
    }

    /**
     * Creates a new UniqueConstraint with default settings (enabled, not deferred).
     *
     * @param name the constraint name (may be null)
     * @param columns the columns in the unique constraint (must not be empty)
     */
    public UniqueConstraint(String name, List<String> columns) {
        this(name, columns, true, false);
    }

    /**
     * Returns the columns in the unique constraint.
     *
     * @return an unmodifiable list of column names
     */
    public List<String> getColumns() {
        return columns;
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.UNIQUE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UniqueConstraint that = (UniqueConstraint) o;
        return columns.equals(that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns);
    }

    @Override
    public String toString() {
        return "UNIQUE (" + String.join(", ", columns) + ")";
    }

    /** Builder for creating UniqueConstraint instances. */
    public static class Builder {
        private final java.util.List<String> columns = new java.util.ArrayList<>();
        private String name;
        private boolean enabled = true;
        private boolean deferred = false;

        /**
         * Adds a column to the unique constraint.
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
         * Builds the UniqueConstraint.
         *
         * @return a new UniqueConstraint
         */
        public UniqueConstraint build() {
            return new UniqueConstraint(name, columns, enabled, deferred);
        }
    }
}
