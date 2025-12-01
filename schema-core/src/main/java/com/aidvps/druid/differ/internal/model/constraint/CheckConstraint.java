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

import java.util.Objects;

/**
 * Represents a check constraint.
 *
 * <p>A check constraint specifies a condition that must be satisfied by all values in a column or
 * set of columns.
 */
public final class CheckConstraint extends Constraint {
    private final String expression;

    /**
     * Creates a new CheckConstraint.
     *
     * @param name the constraint name (may be null)
     * @param expression the SQL expression that defines the check condition
     * @param enabled whether the constraint is enabled
     * @param deferred whether the constraint is deferred
     */
    public CheckConstraint(String name, String expression, boolean enabled, boolean deferred) {
        super(name, enabled, deferred);
        Objects.requireNonNull(expression, "Check constraint expression cannot be null");
        this.expression = expression;
    }

    /**
     * Creates a new CheckConstraint with default settings (enabled, not deferred).
     *
     * @param name the constraint name (may be null)
     * @param expression the SQL expression that defines the check condition
     */
    public CheckConstraint(String name, String expression) {
        this(name, expression, true, false);
    }

    /**
     * Returns the check constraint expression.
     *
     * @return the SQL expression (never null)
     */
    public String getExpression() {
        return expression;
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.CHECK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckConstraint that = (CheckConstraint) o;
        return expression.equals(that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }

    @Override
    public String toString() {
        return "CHECK (" + expression + ")";
    }

    /** Builder for creating CheckConstraint instances. */
    public static class Builder {
        private String name;
        private String expression;
        private boolean enabled = true;
        private boolean deferred = false;

        /**
         * Sets the check constraint expression.
         *
         * @param expression the SQL expression
         * @return this Builder for chaining
         */
        public Builder expression(String expression) {
            this.expression = expression;
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
         * Builds the CheckConstraint.
         *
         * @return a new CheckConstraint
         */
        public CheckConstraint build() {
            return new CheckConstraint(name, expression, enabled, deferred);
        }
    }
}
